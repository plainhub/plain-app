#!/usr/bin/env python3.11
"""
Validate the Android AI image search with ai_models/testimages.
Tests single-input TFLite models (exported via ai-edge-torch).

Usage:
    python scripts/test_clip_models.py
"""
import os, json, re, unicodedata
import numpy as np
from PIL import Image
from ai_edge_litert.interpreter import Interpreter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MDIR = os.path.join(ROOT, "ai_models")
TDIR = os.path.join(MDIR, "testimages")
IMG_M = os.path.join(MDIR, "mobileclip_s2_image.tflite")
TXT_M = os.path.join(MDIR, "mobileclip_s2_text.tflite")
TOK_F = os.path.join(MDIR, "tokenizer.json")
IMGS = ["cat.jpg", "cat2.jpg", "cats.jpg", "people.jpg", "scene.jpg"]
MIN_SCORE = 0.12

# === Tokenizer (mirrors CLIPTokenizer.kt) ===
SOT, EOT = 49406, 49407

def _b2u():
    bs = list(range(33,127))+list(range(161,173))+list(range(174,256))
    cs = list(bs); n = 0
    for b in range(256):
        if b not in bs: bs.append(b); cs.append(256+n); n += 1
    return {b: chr(c) for b, c in zip(bs, cs)}
B2U = _b2u()

class Tok:
    def __init__(self, p):
        d = json.load(open(p)); m = d["model"]
        self.v = dict(m["vocab"].items())
        self.mr = {}
        for i, e in enumerate(m["merges"]):
            l, r = e.split(" ", 1) if isinstance(e, str) else e
            self.mr[(l, r)] = i
    def bpe(self, w):
        cs = [B2U[b] for b in w.encode()]
        if len(cs) == 1: return [cs[0]+"</w>"]
        ps = list(cs); ps[-1] += "</w>"
        while len(ps) > 1:
            b, r = -1, float('inf')
            for i in range(len(ps)-1):
                rk = self.mr.get((ps[i], ps[i+1]))
                if rk is not None and rk < r: r, b = rk, i
            if b < 0: break
            ps[b] += ps[b+1]; del ps[b+1]
        return ps
    def enc(self, t, ml=77):
        t = unicodedata.normalize("NFC", t)
        t = re.sub(r"\s+", " ", t).strip().lower()
        ids = [SOT]
        for w in re.findall(r"[\w]+|[\d]|[^\s\w\d]+", t):
            for tk in self.bpe(w):
                tid = self.v.get(tk)
                if tid is not None: ids.append(tid)
                if len(ids) >= ml-1: break
            if len(ids) >= ml-1: break
        ids.append(EOT)
        o = np.zeros(ml, np.int64); o[:len(ids)] = ids
        return o


# === Image preprocessing (mirrors FIXED ImageEmbedHelper.kt) ===
def prep_image(path, sz=256):
    """Scale to [0,1] only — MobileCLIP-S2 uses identity normalization."""
    img = Image.open(path).convert("RGB")
    w, h = img.size
    scale = sz / min(w, h)
    nw, nh = max(int(w*scale), sz), max(int(h*scale), sz)
    img = img.resize((nw, nh), Image.BILINEAR)
    l, t = (nw-sz)//2, (nh-sz)//2
    img = img.crop((l, t, l+sz, t+sz))
    arr = np.array(img, np.float32) / 255.0
    return arr.transpose(2, 0, 1)[np.newaxis]  # [1,3,H,W]


def l2n(v):
    n = np.linalg.norm(v)
    return v / n if n > 0 else v


def main():
    tok = Tok(TOK_F)

    print("Loading models...")
    im = Interpreter(model_path=IMG_M); im.allocate_tensors()
    tm = Interpreter(model_path=TXT_M); tm.allocate_tensors()

    # Single-input models: 1 input, 1 output each
    im_inp = im.get_input_details()
    im_out = im.get_output_details()
    tm_inp = tm.get_input_details()
    tm_out = tm.get_output_details()

    # Image embeddings
    img_embs = {}
    for name in IMGS:
        p = prep_image(os.path.join(TDIR, name))
        im.set_tensor(im_inp[0]['index'], p.astype(np.float32))
        im.invoke()
        emb = im.get_tensor(im_out[0]['index']).flatten()
        if np.any(np.isnan(emb)) or np.any(np.isinf(emb)):
            print(f"  WARNING: {name} has invalid embedding values!")
            continue
        img_embs[name] = l2n(emb)

    # Text search
    queries = {
        "cat": {"expected": ["cat.jpg","cat2.jpg","cats.jpg"],
                "excluded": ["people.jpg","scene.jpg"]},
        "people": {"expected": ["people.jpg"],
                   "excluded": ["cat.jpg","cat2.jpg","cats.jpg"]},
    }

    all_ok = True
    for q, spec in queries.items():
        tokens = tok.enc(q)[np.newaxis]
        tm.set_tensor(tm_inp[0]['index'], tokens.astype(np.int64))
        tm.invoke()
        txt_emb = l2n(tm.get_tensor(tm_out[0]['index']).flatten())

        sims = {n: float(np.dot(img_embs[n], txt_emb))
                for n in IMGS if n in img_embs}
        results = {n: s for n, s in sims.items() if s >= MIN_SCORE}

        print(f"\nQuery: '{q}' (MIN_SCORE={MIN_SCORE})")
        for n in sorted(sims, key=lambda x: sims[x], reverse=True):
            tag = "✓ MATCH" if n in results else "  skip"
            extra = " (expected)" if n in spec["expected"] else ""
            extra += " (should NOT match)" if n in spec["excluded"] else ""
            print(f"  {n:15s} = {sims[n]:.4f}  {tag}{extra}")

        for n in spec["expected"]:
            if n not in results:
                print(f"  ✗ MISS: {n} not found (score={sims.get(n,'N/A')})")
                all_ok = False
        for n in spec["excluded"]:
            if n in results:
                print(f"  ✗ FALSE POSITIVE: {n} should not match")
                all_ok = False

    print(f"\n{'='*60}")
    print("✓ ALL TESTS PASSED" if all_ok else "✗ SOME TESTS FAILED")
    print(f"{'='*60}")
    return all_ok


if __name__ == "__main__":
    import sys
    sys.exit(0 if main() else 1)
