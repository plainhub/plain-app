#!/usr/bin/env python3.11
"""
End-to-end test for CLIP image search pipeline.

Replicates the exact Kotlin preprocessing and verifies:
  1. cat image matches "cat" better than "dog"
  2. dog image matches "dog" better than "cat"
  3. cosine similarity is above MIN_SCORE threshold

Requires: ai_models/*.tflite (re-exported via ai-edge-torch)

Usage:
    python scripts/test_clip_pipeline.py
"""
import json
import os
import re
import sys
import time
import unicodedata
import urllib.request

import numpy as np
from ai_edge_litert.interpreter import Interpreter

MODEL_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "ai_models")
MIN_SCORE = 0.15

# CLIP tokenizer constants
SOT_ID = 49406
EOT_ID = 49407


def l2_normalize(v):
    n = np.linalg.norm(v)
    return v / n if n > 0 else v


def download_image(url, path):
    if os.path.exists(path):
        return
    print(f"  Downloading {os.path.basename(path)}...")
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, timeout=30) as resp:
        with open(path, "wb") as f:
            f.write(resp.read())


def load_and_preprocess(path, size=256):
    """Load image → resize → center-crop → [0,1] → NCHW float32.
    MobileCLIP-S2 uses identity normalization — just scale to [0,1]."""
    from PIL import Image
    img = Image.open(path).convert("RGB")
    w, h = img.size
    scale = size / min(w, h)
    new_w = max(int(w * scale), size)
    new_h = max(int(h * scale), size)
    img = img.resize((new_w, new_h), Image.BILINEAR)
    w, h = img.size
    left, top = (w - size) // 2, (h - size) // 2
    img = img.crop((left, top, left + size, top + size))
    arr = np.array(img, dtype=np.float32) / 255.0  # [0,1] only
    return arr.transpose(2, 0, 1).reshape(1, 3, size, size)


# --- Tokenizer (mirrors CLIPTokenizer.kt) ---
def _b2u():
    bs = list(range(33, 127)) + list(range(161, 173)) + list(range(174, 256))
    cs = list(bs); n = 0
    for b in range(256):
        if b not in bs: bs.append(b); cs.append(256 + n); n += 1
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
        if len(cs) == 1: return [cs[0] + "</w>"]
        ps = list(cs); ps[-1] += "</w>"
        while len(ps) > 1:
            b, r = -1, float('inf')
            for i in range(len(ps) - 1):
                rk = self.mr.get((ps[i], ps[i + 1]))
                if rk is not None and rk < r: r, b = rk, i
            if b < 0: break
            ps[b] += ps[b + 1]; del ps[b + 1]
        return ps

    def enc(self, t, ml=77):
        t = unicodedata.normalize("NFC", t)
        t = re.sub(r"\s+", " ", t).strip().lower()
        ids = [SOT_ID]
        for w in re.findall(r"[\w]+|[\d]|[^\s\w\d]+", t):
            for tk in self.bpe(w):
                tid = self.v.get(tk)
                if tid is not None: ids.append(tid)
                if len(ids) >= ml - 1: break
            if len(ids) >= ml - 1: break
        ids.append(EOT_ID)
        o = np.zeros(ml, np.int64); o[:len(ids)] = ids
        return o


def embed_image(interp, image_nchw):
    """Single-input image model: pixel_values → embedding."""
    inp = interp.get_input_details()
    out = interp.get_output_details()
    interp.set_tensor(inp[0]["index"], image_nchw.astype(np.float32))
    interp.invoke()
    emb = interp.get_tensor(out[0]["index"]).flatten()
    return l2_normalize(emb)


def embed_text(interp, tokens):
    """Single-input text model: input_ids → embedding."""
    inp = interp.get_input_details()
    out = interp.get_output_details()
    interp.set_tensor(inp[0]["index"], tokens.astype(np.int64))
    interp.invoke()
    emb = interp.get_tensor(out[0]["index"]).flatten()
    return l2_normalize(emb)


def main():
    test_dir = "/tmp/clip_test_images"
    os.makedirs(test_dir, exist_ok=True)

    print("Downloading test images...")
    cat_path = os.path.join(test_dir, "cat.jpg")
    dog_path = os.path.join(test_dir, "dog.jpg")
    download_image(
        "https://cdn.pixabay.com/photo/2014/11/30/14/11/cat-551554_640.jpg",
        cat_path)
    download_image(
        "https://cdn.pixabay.com/photo/2016/02/19/15/46/labrador-retriever-1210559_640.jpg",
        dog_path)

    print("Preprocessing images...")
    cat_nchw = load_and_preprocess(cat_path)
    dog_nchw = load_and_preprocess(dog_path)

    img_interp = Interpreter(os.path.join(MODEL_DIR, "mobileclip_s2_image.tflite"))
    img_interp.allocate_tensors()
    txt_interp = Interpreter(os.path.join(MODEL_DIR, "mobileclip_s2_text.tflite"))
    txt_interp.allocate_tensors()
    tok = Tok(os.path.join(MODEL_DIR, "tokenizer.json"))

    cat_emb = embed_image(img_interp, cat_nchw)
    dog_emb = embed_image(img_interp, dog_nchw)

    queries = ["cat", "dog", "car", "sunset", "a photo of a cat"]
    text_embs = {}
    for q in queries:
        text_embs[q] = embed_text(txt_interp, tok.enc(q)[np.newaxis])

    print(f"\n{'Query':<25s}  {'cat_img':>8s}  {'dog_img':>8s}")
    print("-" * 45)
    for q in queries:
        sc = float(np.dot(cat_emb, text_embs[q]))
        sd = float(np.dot(dog_emb, text_embs[q]))
        print(f"  {q:<23s}  {sc:+.4f}    {sd:+.4f}")

    print("\nTESTS:")
    passed = failed = 0

    def check(name, cond):
        nonlocal passed, failed
        if cond: print(f"  PASS: {name}"); passed += 1
        else: print(f"  FAIL: {name}"); failed += 1

    scc = float(np.dot(cat_emb, text_embs["cat"]))
    scd = float(np.dot(cat_emb, text_embs["dog"]))
    sdd = float(np.dot(dog_emb, text_embs["dog"]))
    sdc = float(np.dot(dog_emb, text_embs["cat"]))
    check(f"cat×'cat' ({scc:.4f}) > MIN_SCORE ({MIN_SCORE})", scc > MIN_SCORE)
    check(f"cat×'cat' ({scc:.4f}) > cat×'dog' ({scd:.4f})", scc > scd)
    check(f"dog×'dog' ({sdd:.4f}) > MIN_SCORE ({MIN_SCORE})", sdd > MIN_SCORE)
    check(f"dog×'dog' ({sdd:.4f}) > dog×'cat' ({sdc:.4f})", sdd > sdc)
    check(f"embeddings are 512-dim", cat_emb.shape == (512,))
    print(f"\n{passed} passed, {failed} failed")
    return 0 if failed == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
