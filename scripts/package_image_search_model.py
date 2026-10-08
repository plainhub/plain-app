#!/usr/bin/env python3
import argparse
import hashlib
import json
from pathlib import Path
import shutil


def main():
    parser = argparse.ArgumentParser(description="Package existing paired ONNX encoders for image search")
    parser.add_argument("--image", type=Path, required=True)
    parser.add_argument("--text", type=Path, required=True)
    parser.add_argument("--tokenizer", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--id", required=True)
    parser.add_argument("--name", required=True)
    parser.add_argument("--license", required=True)
    parser.add_argument("--source", required=True)
    parser.add_argument("--dimensions", type=int, required=True)
    parser.add_argument("--image-input", required=True)
    parser.add_argument("--image-output", required=True)
    parser.add_argument("--text-output", required=True)
    parser.add_argument("--text-input", action="append", required=True, help="ONNX input name:ids|attentionMask|typeIds")
    parser.add_argument("--context-length", type=int, required=True)
    parser.add_argument("--pad-token", required=True)
    parser.add_argument("--size", type=int, required=True)
    parser.add_argument("--resize", choices=["centerCrop", "stretch"], required=True)
    parser.add_argument("--layout", choices=["nchw", "nhwc"], default="nchw")
    parser.add_argument("--interpolation", choices=["bilinear", "bicubic"], required=True)
    parser.add_argument("--mean", type=float, nargs=3, required=True)
    parser.add_argument("--std", type=float, nargs=3, required=True)
    parser.add_argument("--minimum-score", type=float, required=True)
    args = parser.parse_args()
    if args.output.exists():
        raise SystemExit("Output directory must not already exist")
    inputs = []
    for item in args.text_input:
        name, role = item.rsplit(":", 1)
        if role not in ["ids", "attentionMask", "typeIds"]:
            raise SystemExit("Invalid text input role")
        inputs.append({"name": name, "role": role})
    declaration = {
        "formatVersion": 1, "id": args.id, "name": args.name,
        "license": args.license, "source": args.source,
        "dimensions": args.dimensions, "minimumScore": args.minimum_score,
        "files": [],
        "image": {"file": "image.onnx", "input": args.image_input, "output": args.image_output},
        "text": {"file": "text.onnx", "inputs": inputs, "output": args.text_output},
        "tokenizer": {"file": "tokenizer.json", "contextLength": args.context_length, "padToken": args.pad_token},
        "preprocess": {"size": args.size, "resize": args.resize, "layout": args.layout,
                       "interpolation": args.interpolation, "mean": args.mean, "std": args.std},
    }
    sources = [(args.image, "image.onnx"), (args.text, "text.onnx"), (args.tokenizer, "tokenizer.json")]
    for source, name in sources:
        if not source.is_file():
            raise SystemExit("Missing input: " + str(source))
        digest = hashlib.sha256()
        with source.open("rb") as data:
            for chunk in iter(lambda: data.read(65536), b""):
                digest.update(chunk)
        checksum = digest.hexdigest()
        declaration["files"].append({"name": name, "size": source.stat().st_size, "sha256": checksum})
    args.output.mkdir(parents=True)
    try:
        for source, name in sources:
            shutil.copyfile(source, args.output / name)
        (args.output / "manifest.json").write_text(json.dumps(declaration, indent=2, ensure_ascii=False), encoding="utf-8")
    except BaseException:
        shutil.rmtree(args.output)
        raise
    print(args.output.resolve())


if __name__ == "__main__":
    main()
