#!/usr/bin/env python3.11
"""
Export MobileCLIP-S2 to separate image/text TFLite models for Android.

Uses ai-edge-torch (PyTorch → TFLite directly). No ONNX.

Install dependencies:
    pip install open_clip_torch torch numpy litert-torch tensorflow

Usage:
    python scripts/export_clip_tflite.py

Output:
    ai_models/mobileclip_s2_image.tflite  (image encoder)
    ai_models/mobileclip_s2_text.tflite   (text encoder)
    ai_models/tokenizer.json              (BPE vocab+merges)

Notes:
  - MobileCLIP-S2 uses identity normalization: mean=(0,0,0) std=(1,1,1).
    Images should be scaled to [0,1] only — no CLIP mean/std.
  - Image input:  [1, 3, 256, 256] float32 (NCHW)
  - Text input:   [1, 77] int64 (token IDs)
  - Both outputs: [1, 512] float32 (embedding)
"""

import json
import os

import numpy as np
import torch

MODEL_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "ai_models")


class ImageEncoder(torch.nn.Module):
    """Wraps model.visual to accept [1,3,256,256] → [1,512]."""
    def __init__(self, visual):
        super().__init__()
        self.visual = visual

    def forward(self, pixel_values):
        return self.visual(pixel_values)


class TextEncoder(torch.nn.Module):
    """Text encoder replacing argmax (unsupported in TFLite) with EOT mask pooling.
    Inputs:  input_ids [1, 77] int64
    Output:  embedding [1, 512] float32"""
    def __init__(self, clip_model):
        super().__init__()
        t = clip_model.text
        self.token_embedding = t.token_embedding
        self.positional_embedding = t.positional_embedding
        self.transformer = t.transformer
        self.ln_final = t.ln_final
        self.text_projection = t.text_projection

    def forward(self, input_ids):
        # _embeds equivalent: NLD format (transformer handles LND internally)
        cast_dtype = self.transformer.get_cast_dtype()
        x = self.token_embedding(input_ids).to(cast_dtype)
        x = x + self.positional_embedding.to(cast_dtype)
        x = self.transformer(x, attn_mask=None)
        x = self.ln_final(x)
        # Mask-based EOT selection: avoids tfl.arg_max (marked illegal in TFLite)
        # EOT token (49407) is the highest ID so argmax == this mask for valid inputs
        eot_mask = (input_ids == 49407).float().unsqueeze(-1)  # [1, 77, 1]
        pooled = (x * eot_mask).sum(dim=1)                     # [1, D]
        if self.text_projection is not None:
            pooled = pooled @ self.text_projection
        return pooled


def export_models():
    """Export MobileCLIP-S2 image & text encoders via ai-edge-torch."""
    import litert_torch
    import open_clip

    os.makedirs(MODEL_DIR, exist_ok=True)

    print("Loading MobileCLIP-S2 model...")
    model, _, _ = open_clip.create_model_and_transforms(
        "MobileCLIP-S2", pretrained="datacompdr"
    )
    model.eval()

    # --- Image encoder ---
    img_path = os.path.join(MODEL_DIR, "mobileclip_s2_image.tflite")
    if os.path.exists(img_path):
        print(f"Skipping image encoder (already exists: {os.path.getsize(img_path)/1e6:.1f} MB)")
    else:
        print("Converting image encoder → TFLite...")
        img_enc = ImageEncoder(model.visual)
        img_enc.eval()
        dummy_image = torch.randn(1, 3, 256, 256)
        edge_img = litert_torch.convert(img_enc, (dummy_image,))
        edge_img.export(img_path)
        sz = os.path.getsize(img_path)
        print(f"  → {img_path} ({sz / 1e6:.1f} MB)")

    # --- Text encoder ---
    print("Converting text encoder → TFLite...")
    txt_enc = TextEncoder(model)
    txt_enc.eval()
    dummy_text = torch.randint(0, 49408, (1, 77), dtype=torch.long)
    edge_txt = litert_torch.convert(txt_enc, (dummy_text,))
    txt_path = os.path.join(MODEL_DIR, "mobileclip_s2_text.tflite")
    edge_txt.export(txt_path)
    sz = os.path.getsize(txt_path)
    print(f"  → {txt_path} ({sz / 1e6:.1f} MB)")

    # --- Tokenizer ---
    print("Exporting tokenizer.json...")
    export_tokenizer()

    # --- Validate ---
    print("\nValidating exported models...")
    validate(model)

    print("\nDone! Models saved to:", MODEL_DIR)


def validate(pytorch_model):
    """Quick sanity check: compare TFLite output to PyTorch reference."""
    from ai_edge_litert.interpreter import Interpreter

    img_path = os.path.join(MODEL_DIR, "mobileclip_s2_image.tflite")
    txt_path = os.path.join(MODEL_DIR, "mobileclip_s2_text.tflite")

    # Image embedding comparison
    dummy_image = torch.randn(1, 3, 256, 256)
    with torch.no_grad():
        ref_img = pytorch_model.visual(dummy_image).numpy().flatten()
    ref_img = ref_img / np.linalg.norm(ref_img)

    interp = Interpreter(model_path=img_path)
    interp.allocate_tensors()
    inp = interp.get_input_details()
    out = interp.get_output_details()
    interp.set_tensor(inp[0]["index"], dummy_image.numpy())
    interp.invoke()
    tfl_img = interp.get_tensor(out[0]["index"]).flatten()
    tfl_img = tfl_img / np.linalg.norm(tfl_img)
    cos_img = float(np.dot(ref_img, tfl_img))
    print(f"  Image encoder cos(pytorch, tflite) = {cos_img:.6f}")
    assert cos_img > 0.99, f"Image encoder mismatch: {cos_img}"

    # Text embedding comparison — use proper tokenized input with EOT token
    dummy_text = torch.zeros(1, 77, dtype=torch.long)
    dummy_text[0, 0] = 49406  # SOT
    dummy_text[0, 1] = 1234   # sample token
    dummy_text[0, 2] = 49407  # EOT
    with torch.no_grad():
        ref_txt = pytorch_model.encode_text(dummy_text).numpy().flatten()
    ref_txt = ref_txt / np.linalg.norm(ref_txt)

    interp2 = Interpreter(model_path=txt_path)
    interp2.allocate_tensors()
    inp2 = interp2.get_input_details()
    out2 = interp2.get_output_details()
    interp2.set_tensor(inp2[0]["index"], dummy_text.numpy())
    interp2.invoke()
    tfl_txt = interp2.get_tensor(out2[0]["index"]).flatten()
    tfl_txt = tfl_txt / np.linalg.norm(tfl_txt)
    cos_txt = float(np.dot(ref_txt, tfl_txt))
    print(f"  Text encoder  cos(pytorch, tflite) = {cos_txt:.6f}")
    assert cos_txt > 0.99, f"Text encoder mismatch: {cos_txt}"


def export_tokenizer():
    """Export OpenCLIP's tokenizer vocab + merges as tokenizer.json."""
    import open_clip

    open_clip.get_tokenizer("MobileCLIP-S2")
    from open_clip.tokenizer import SimpleTokenizer
    simple_tok = SimpleTokenizer()

    vocab = {k: v for k, v in simple_tok.encoder.items()}
    merges = [f"{p[0]} {p[1]}" for p in simple_tok.bpe_ranks.keys()]

    tokenizer_json = {"model": {"vocab": vocab, "merges": merges}}
    out_path = os.path.join(MODEL_DIR, "tokenizer.json")
    with open(out_path, "w") as f:
        json.dump(tokenizer_json, f)
    print(f"  → {out_path}")


if __name__ == "__main__":
    export_models()
