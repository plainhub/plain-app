# helpers/ — Stateless Utility Helpers

> 31 files | Package: `com.ismartcoding.plain.helpers`

## File Operations
| File | Purpose |
|------|---------|
| FileHelper.kt | File I/O, copy, URI handling, MediaStore scan, Base64 encode/decode |
| FileHashHelper.kt | Two-step dedup: weakHash (first+last 4KB SHA-256), strongHash (full SHA-256) |
| PathHelper.kt | Public dir paths: `{ExternalStorage}/{dir}/PlainApp` |
| TempHelper.kt | In-memory key-value temp store (no persistence) |
| TextFileHelper.kt | Line counting, chunked file reading (8KB) |
| DownloadHelper.kt | HTTP download → disk with SHA-1 path from URL |

## App & Device
| File | Purpose |
|------|---------|
| AppHelper.kt | Version checking, update detection, HTTP client for release checks |
| AppLogHelper.kt | Log folder size, zip logs, share via FileProvider |
| DeviceInfoHelper.kt | Device name, Android version, security patch, phone numbers, SIM |
| PhoneHelper.kt | Device name (Settings/Bluetooth), device type, battery, screen timeout |
| AppFileStore.kt | Content-addressable chat file store (`fid:` URI scheme), SHA-256 paths |

## Media
| File | Purpose |
|------|---------|
| BitmapHelper.kt | Bitmap decode with scaling, `calculateInSampleSize()` |
| ImageHelper.kt | GIF/WebP/PNG/JPEG detection via file signature, EXIF rotation |
| VideoHelper.kt | Video metadata: width, height, rotation, duration, bitrate, frameRate |
| Mp4Helper.kt | 3GP→MP4 conversion (H.263+AMR-NB → H.264+AAC) for browser |
| SvgHelper.kt | SVG size extraction via androidsvg |

## Chat
| File | Purpose |
|------|---------|
| ChatFileSaveHelper.kt | Import files from content-resolver → content-addressable store |

## QR / Barcode
| File | Purpose |
|------|---------|
| QrCodeGenerateHelper.kt | ZXing QR code → Bitmap generation |
| QrCodeScanHelper.kt | Multi-resolution ZXing QR decode (1500px→100px) |
| QrCodeBitmapHelper.kt | Load QR source bitmap from URI (1024px max) |

## Formatting & Time
| File | Purpose |
|------|---------|
| FormatHelper.kt | `formatSeconds()`, byte sizing, currency, locale-aware numbers |
| TimeAgoHelper.kt | Relative time: ms → "2 hours ago" with plural support |
| TimeHelper.kt | `now()` → Instant |

## Notification & Share
| File | Purpose |
|------|---------|
| NotificationHelper.kt | Create notifications, channels (Android 8+), remote input, stop action |
| NotificationsHelper.kt | Notification filtering: allowlist/blacklist per app |
| ShareHelper.kt | Android share intent, MIME detection, FileProvider |
| ScreenHelper.kt | Keep screen on toggle via WRITE_SETTINGS |

## Crypto & Security
| File | Purpose |
|------|---------|
| SignatureHelper.kt | Ed25519 sign data/text → Base64, get 32-byte public key |

## Query
| File | Purpose |
|------|---------|
| QueryHelper.kt | Parse tag_id filters → tag lookup → ID replacement |
| UrlHelper.kt | Media HTTP URLs, cast callback URL, health check URL |
| SoundMeterHelper.kt | Audio amplitude → decibel conversion |
