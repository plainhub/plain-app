# lib/ — Shared Utility Library

> Package: `com.ismartcoding.lib`

## Top-Level
- **Constants.kt** — Library constants
- **Version.kt** — Version info

## helpers/ — Core Utilities (12 files)

| File | Purpose |
|------|---------|
| CoroutinesHelper.kt | `withIO()`, `coIO()`, `coMain()` dispatchers; `pmap()` parallel mapping |
| CryptoHelper.kt | XChaCha20-Poly1305 (AEAD), Ed25519, RSA, ECDH, PBKDF2, SHA-256/SHA-1/MD5 |
| JsonHelper.kt | kotlinx-serialization `jsonEncode()`→String, `jsonDecode()`→T |
| NetworkHelper.kt | `getDeviceIP4()` non-VPN IPv4, `isVpnInterface()` |
| SearchHelper.kt | FilterField query parsing: `:`, `!=`, `>=`, `>`, `<=`, `<`, `in`, `nin` |
| SslHelper.kt | SSL bypass for dev: trust-all certs, disable hostname verification |
| JksHelper.kt | `genJksFile()` self-signed ECDSA P-256 cert (10yr validity) |
| PortHelper.kt | `isPortInUse()` ServerSocket bind check |
| StringHelper.kt | `shortUUID()` base-36, `getQuestionMarks()` SQL placeholders |
| XmlHelper.kt | `parseData<T>()` via GsonXml, `extractFirstChildOfBody()` SOAP |
| ZipHelper.kt | `zip()` compress file list, `unzip()` extract |
| AssetsHelper.kt | Load text from assets folder |

## channel/ — Event Bus
- `sendEvent()` / `receiveEvent<T>()` channel-based pub/sub

## extensions/ — Kotlin Extensions (11 files)
Bitmap, Bundle, ContentResolver, Context, Cursor, HttpResponse, Intent, List, Long, String, Uri

## Bundled Libraries
| Directory | What |
|-----------|------|
| ahocorasick/ | Multi-pattern string matching |
| androidsvg/ | SVG rendering |
| apk/ | APK parsing (ApkParsers.kt) |
| gsonxml/ | XML→JSON (GsonXml, XmlReader) |
| html2md/ | HTML→Markdown converter |
| logcat/ | Logging framework (LogCat, DiskLogAdapter) |
| markdown/ | Markdown utilities |
| mustache/ | Mustache templating |
| opml/ | OPML parsing |
| pdfviewer/ | PDF rendering |
| phonegeo/ | Phone number geolocation |
| pinyin/ | Chinese → Pinyin conversion |
| readability4j/ | Article content extraction |
| rss/ | RSS/Atom feed parsing |
| upnp/ | DLNA/UPnP support |
