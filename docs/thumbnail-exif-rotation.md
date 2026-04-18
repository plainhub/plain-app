# Thumbnail EXIF Orientation

## Problem

JPEG/HEIC files taken by cameras store pixels in the **sensor's native orientation**
(usually landscape for phone cameras) and embed an EXIF `Orientation` tag that tells
viewers how to rotate/flip the image for correct display.

`BitmapFactory.decodeFile()` — used in our fast thumbnail path — **always ignores this
tag**, regardless of Android version. Only two Android APIs apply it automatically:

| API | Auto-applies EXIF? |
|---|---|
| `BitmapFactory.decodeFile()` | ❌ Never |
| `ImageDecoder` (API 28+) | ✅ Yes |
| `ContentResolver.loadThumbnail()` | ✅ Yes |
| Coil 3 file decoder | ✅ Yes (uses `ImageDecoder` internally) |

Because `ThumbnailGenerator` uses `BitmapFactory` for the fast path, thumbnails for
rotated images (e.g., portrait iPhone photos with EXIF orientation 6) appeared sideways.

## Affected Code Path

```
/fs?id=xxx&w=1024&h=1024
  → ThumbnailGenerator.toThumbBytesAsync()
    → Priority 1 (MediaStore, width ≤ 512): SKIPPED for w=1024
    → Priority 2 (disk cache): may serve stale thumbnails
    → Priority 3: getBitmapAsync() → decodeSampledBitmapFromFile()
                                       ← BitmapFactory, EXIF NOT applied!
```

## Fix: `ThumbnailDecoder.kt`

### 1. Read EXIF orientation once before decoding

```kotlin
val exifOrient = readExifOrientation(path)  // reads TAG_ORIENTATION from file
```

### 2. Swap requested dimensions for 90°/270° orientations

Raw pixels for a 3264×2448 landscape-stored portrait photo have **transposed axes**.
If we compute `inSampleSize` using portrait dimensions (reqW=1024, reqH=1024) against
landscape raw pixels (srcW=3264, srcH=2448), the sample size is correct for square
requests but wrong for non-square ones. Swap `reqW`/`reqH` when computing `inSampleSize`:

```kotlin
val swapDims = exifOrient in listOf(
    ORIENTATION_ROTATE_90, ORIENTATION_ROTATE_270,
    ORIENTATION_TRANSPOSE, ORIENTATION_TRANSVERSE
)
val logicalReqW = if (swapDims) reqHeight else reqWidth
val logicalReqH = if (swapDims) reqWidth else reqHeight
```

### 3. Apply full transform matrix BEFORE center-crop

All 8 EXIF orientation values are handled via `applyExifOrientation()`.
The transform is applied **after decoding but before the center-crop** so the crop
operates on the correctly-oriented image:

| Value | Name | Transform |
|---|---|---|
| 1 | NORMAL | none |
| 2 | FLIP_HORIZONTAL | flip X |
| 3 | ROTATE_180 | rotate 180° |
| 4 | FLIP_VERTICAL | flip Y |
| 5 | TRANSPOSE | rotate 90° CW + flip X |
| 6 | ROTATE_90 | rotate 90° CW |
| 7 | TRANSVERSE | rotate 270° CW + flip X |
| 8 | ROTATE_270 | rotate 270° CW |

## Fix: `ThumbnailCache.kt` — Cache Version Bump

Old (incorrectly oriented) thumbnails may already be stored in the disk cache.
The cache key includes a `CACHE_VERSION` constant. **Bump it whenever the decode
algorithm changes** to force regeneration:

```kotlin
private const val CACHE_VERSION = "v2"  // v1→v2: EXIF orientation fix

private fun cacheKey(...): String {
    val raw = "$path:$lastModifiedMs:${w}x$h:$centerCrop:$CACHE_VERSION"
    ...
}
```

Without this bump, users who already cached a thumbnail would see the old wrong version
until the source file's mtime changes (e.g., after editing the file).

## How Coil Handles This

The project's custom `ThumbnailDecoder` (in `ui/base/coil/`) uses
`ContentResolver.loadThumbnail()` which applies EXIF automatically.

For the fallback Coil path in `ThumbnailGenerator` (loading from `File`), Coil 3
uses `ImageDecoder` on Android 9+ which also auto-applies EXIF. So **Coil was always
correct**; only our BitmapFactory path needed the fix.

## Verification Script

```bash
# Test specific file
python3 scripts/test_thumbnail_exif.py ~/Downloads/IMG_1423.JPG

# Test all JPEGs in Downloads/
python3 scripts/test_thumbnail_exif.py
```

The script simulates the exact Kotlin decode pipeline in Python and compares output
against PIL's `exif_transpose` (ground truth). All 8 EXIF orientation values are
covered by synthetic test images.

## Prevention Checklist

When changing thumbnail generation code:

1. **Never** call `BitmapFactory.decodeFile()` without applying `applyExifOrientation()` afterwards.
2. **Always** swap reqW/reqH when computing `inSampleSize` for rotated (90°/270°) images.
3. **Bump `CACHE_VERSION`** in `ThumbnailCache` whenever the decode/transform algorithm changes.
4. **Run the verification script** to confirm all 8 orientations still pass.
