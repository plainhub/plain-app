#!/usr/bin/env python3
"""
Verify that the ThumbnailDecoder EXIF orientation fix produces correctly-oriented thumbnails.

Simulates the Kotlin algorithm (BitmapFactory path) in Python:
  1. Read raw pixels without auto-rotation (as BitmapFactory does)
  2. Read EXIF orientation tag
  3. Apply the same matrix transform as applyExifOrientation()
  4. Compare with PIL's exif_transpose (ground truth)

Usage:
  python3 scripts/test_thumbnail_exif.py ~/Downloads/IMG_1423.JPG
  python3 scripts/test_thumbnail_exif.py  # tests all JPEGs in ~/Downloads/
"""

import sys
import os
import hashlib
from pathlib import Path
from PIL import Image, ImageOps, ImageChops
import piexif

# ---------------------------------------------------------------------------
# Mirror of Kotlin applyExifOrientation() using PIL operations
# ---------------------------------------------------------------------------
ORIENTATION_NORMAL           = 1
ORIENTATION_FLIP_HORIZONTAL  = 2
ORIENTATION_ROTATE_180       = 3
ORIENTATION_FLIP_VERTICAL    = 4
ORIENTATION_TRANSPOSE        = 5   # rotate 90° CW + flip X
ORIENTATION_ROTATE_90        = 6   # rotate 90° CW
ORIENTATION_TRANSVERSE       = 7   # rotate 270° CW + flip X
ORIENTATION_ROTATE_270       = 8   # rotate 270° CW


def apply_exif_orientation(img: Image.Image, orient: int) -> Image.Image:
    """
    Apply the EXIF orientation transform exactly as applyExifOrientation() in Kotlin.
    PIL rotate() is CCW; negative angle = CW.
    """
    if orient in (ORIENTATION_NORMAL, 0):
        return img
    elif orient == ORIENTATION_FLIP_HORIZONTAL:
        return img.transpose(Image.FLIP_LEFT_RIGHT)
    elif orient == ORIENTATION_ROTATE_180:
        return img.rotate(180, expand=True)
    elif orient == ORIENTATION_FLIP_VERTICAL:
        return img.transpose(Image.FLIP_TOP_BOTTOM)
    elif orient == ORIENTATION_TRANSPOSE:          # postRotate(90) + preScale(-1,1)
        rotated = img.rotate(-90, expand=True)     # 90° CW
        return rotated.transpose(Image.FLIP_LEFT_RIGHT)
    elif orient == ORIENTATION_ROTATE_90:
        return img.rotate(-90, expand=True)        # 90° CW
    elif orient == ORIENTATION_TRANSVERSE:         # postRotate(270) + preScale(-1,1)
        rotated = img.rotate(90, expand=True)      # 270° CW = 90° CCW
        return rotated.transpose(Image.FLIP_LEFT_RIGHT)
    elif orient == ORIENTATION_ROTATE_270:
        return img.rotate(90, expand=True)         # 270° CW = 90° CCW
    else:
        return img


def calc_in_sample_size(src_w, src_h, req_w, req_h, fill):
    """Mirror of calcInSampleSize() in Kotlin."""
    size = 1
    while True:
        nxt = size * 2
        nw = src_w // nxt
        nh = src_h // nxt
        if fill:
            fits = nw >= req_w and nh >= req_h
        else:
            fits = nw >= req_w or nh >= req_h
        if not fits or nw <= 0 or nh <= 0:
            break
        size = nxt
    return size


def crop_center(img: Image.Image, req_w, req_h) -> Image.Image:
    """Mirror of cropCenter() in Kotlin."""
    w, h = img.size
    if w == req_w and h == req_h:
        return img
    x = max((w - req_w) // 2, 0)
    y = max((h - req_h) // 2, 0)
    return img.crop((x, y, x + min(req_w, w), y + min(req_h, h)))


def decode_sampled_bitmap(path: str, req_w: int, req_h: int, center_crop: bool = True) -> Image.Image:
    """
    Simulate decodeSampledBitmapFromFile() — raw BitmapFactory decode (no EXIF)
    + EXIF orientation fix + optional center-crop.
    """
    img_raw = Image.open(path)
    src_w, src_h = img_raw.size

    # Read EXIF orientation (raw; do NOT call exif_transpose on this image)
    orient = ORIENTATION_NORMAL
    try:
        exif_bytes = img_raw.info.get("exif", b"")
        if exif_bytes:
            exif = piexif.load(exif_bytes)
            orient = exif.get("0th", {}).get(piexif.ImageIFD.Orientation, ORIENTATION_NORMAL)
    except Exception:
        pass

    swap_dims = orient in (ORIENTATION_ROTATE_90, ORIENTATION_ROTATE_270,
                           ORIENTATION_TRANSPOSE, ORIENTATION_TRANSVERSE)
    logical_req_w = req_h if swap_dims else req_w
    logical_req_h = req_w if swap_dims else req_h

    sample_size = calc_in_sample_size(src_w, src_h, logical_req_w, logical_req_h, center_crop)

    # Coarse downsample (simulate inSampleSize)
    small_w = max(src_w // sample_size, 1)
    small_h = max(src_h // sample_size, 1)
    img_sampled = img_raw.resize((small_w, small_h), Image.BILINEAR)

    # Fine scale (simulate inDensity/inTargetDensity)
    if center_crop:
        scale = max(logical_req_w / small_w, logical_req_h / small_h)
    else:
        scale = min(logical_req_w / small_w, logical_req_h / small_h)
    scale = min(scale, 1.0)
    if scale < 1.0:
        img_sampled = img_sampled.resize(
            (max(1, round(small_w * scale)), max(1, round(small_h * scale))),
            Image.BILINEAR
        )

    # Apply EXIF orientation
    img_oriented = apply_exif_orientation(img_sampled, orient)

    # Center-crop
    if center_crop:
        img_oriented = crop_center(img_oriented, req_w, req_h)

    return img_oriented


def ground_truth_thumb(path: str, req_w: int, req_h: int) -> Image.Image:
    """
    Reference thumbnail using PIL's exif_transpose (equivalent to Coil / ImageDecoder).
    Produces the same centerCrop result as our algorithm should.
    """
    img = ImageOps.exif_transpose(Image.open(path))
    w, h = img.size
    # Scale so both dims are >= req while maintaining aspect ratio (FILL / centerCrop)
    scale = max(req_w / w, req_h / h)
    if scale < 1.0:
        img = img.resize((max(1, round(w * scale)), max(1, round(h * scale))), Image.LANCZOS)
    return crop_center(img, req_w, req_h)


def run_test(path: str, req_sizes=None):
    if req_sizes is None:
        req_sizes = [(1024, 1024), (512, 512), (200, 200), (300, 200), (200, 300)]

    print(f"\n{'='*60}")
    print(f"File: {path}")

    img_raw = Image.open(path)
    src_w, src_h = img_raw.size

    # Get EXIF orientation
    orient = ORIENTATION_NORMAL
    try:
        exif_bytes = img_raw.info.get("exif", b"")
        if exif_bytes:
            exif = piexif.load(exif_bytes)
            orient = exif.get("0th", {}).get(piexif.ImageIFD.Orientation, ORIENTATION_NORMAL)
    except Exception:
        pass

    orient_names = {
        1: "NORMAL", 2: "FLIP_H", 3: "ROT_180", 4: "FLIP_V",
        5: "TRANSPOSE", 6: "ROT_90_CW", 7: "TRANSVERSE", 8: "ROT_270_CW",
    }
    print(f"Raw pixel size: {src_w}×{src_h}  |  EXIF orientation: {orient} ({orient_names.get(orient, '?')})")

    img_correct = ImageOps.exif_transpose(img_raw)
    print(f"Correct (EXIF-transposed) size: {img_correct.size[0]}×{img_correct.size[1]}")

    all_ok = True
    for req_w, req_h in req_sizes:
        thumb = decode_sampled_bitmap(path, req_w, req_h, center_crop=True)
        gt = ground_truth_thumb(path, req_w, req_h)

        # Check 1: output size matches request
        size_ok = thumb.size == gt.size

        # Check 2: pixels match ground truth within tolerance (allow for different
        # downscaling kernels between PIL BILINEAR vs native Android).
        # Use a loose tolerance and a sample of pixels from the corners.
        content_ok = True
        if size_ok:
            tw, th = thumb.size
            # Sample 5 points: center + 4 corners (inset by 5%)
            ix = max(tw // 20, 1)
            iy = max(th // 20, 1)
            points = [(tw // 2, th // 2), (ix, iy), (tw - ix, iy), (ix, th - iy), (tw - ix, th - iy)]
            thumb_rgb = thumb.convert("RGB")
            gt_rgb = gt.convert("RGB")
            max_channel_diff = 0
            for px, py in points:
                tp = thumb_rgb.getpixel((px, py))
                gp = gt_rgb.getpixel((px, py))
                for c in range(3):
                    max_channel_diff = max(max_channel_diff, abs(int(tp[c]) - int(gp[c])))
            # Tolerance: 40 per channel (coarse resampling differences)
            content_ok = max_channel_diff <= 40

        ok = size_ok and content_ok
        status = "✓ PASS" if ok else "✗ FAIL"
        if not ok:
            all_ok = False
            extra = f"  ← size={thumb.size} gt={gt.size}" if not size_ok else f"  ← max_channel_diff={max_channel_diff}"
        else:
            extra = ""
        print(f"  req={req_w}×{req_h}  →  thumb={thumb.size[0]}×{thumb.size[1]}  {status}{extra}")

        # Save for visual inspection
        out_path = f"/tmp/thumb_exif_{Path(path).stem}_{req_w}x{req_h}.jpg"
        thumb.save(out_path)
        gt_path = f"/tmp/thumb_gt_{Path(path).stem}_{req_w}x{req_h}.jpg"
        gt.save(gt_path)

    print(f"Overall: {'✓ ALL PASS' if all_ok else '✗ SOME FAILED'}")
    return all_ok


def main():
    # Install piexif if needed
    try:
        import piexif  # noqa
    except ImportError:
        print("Installing piexif...")
        os.system("pip3 install piexif -q")
        import piexif  # noqa

    if len(sys.argv) > 1:
        paths = [os.path.expanduser(p) for p in sys.argv[1:]]
    else:
        dl = os.path.expanduser("~/Downloads")
        paths = list(Path(dl).glob("*.JPG")) + list(Path(dl).glob("*.jpg"))
        if not paths:
            print("No JPEG files found in ~/Downloads/. Pass a path as argument.")
            sys.exit(1)

    all_pass = True
    for p in paths:
        ok = run_test(str(p))
        all_pass = all_pass and ok

    print(f"\n{'='*60}")
    print(f"FINAL: {'ALL TESTS PASSED ✓' if all_pass else 'SOME TESTS FAILED ✗'}")
    print(f"Visual thumbnails saved to /tmp/thumb_exif_*.jpg")
    sys.exit(0 if all_pass else 1)


if __name__ == "__main__":
    main()
