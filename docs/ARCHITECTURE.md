# PlainApp Architecture

> **Purpose**: AI-friendly project map. Read this first to avoid blind searching.

## Quick Facts

| Item | Value |
|------|-------|
| Package | `com.ismartcoding.plain` |
| Language | Kotlin + Jetpack Compose |
| Min SDK / Target | 28 / 36 |
| Modules | `:app` (main), `:lib` (shared utilities) |
| DB | Room 2.8.4 (`AppDatabase`, 17 entities) |
| Server | Ktor 3.4.1 (HTTP + WebSocket, embedded in app) |
| API | KGraphQL 0.19.0 (GraphQL over HTTP/WebSocket) |
| DI | None (manual construction, singletons, companion objects) |
| Navigation | Compose Navigation |
| State | ViewModel + StateFlow |
| Events | Custom channel-based event bus (`lib/channel/sendEvent`) |
| Crypto | XChaCha20-Poly1305 (Tink), ECDH, Ed25519 |
| Build flavors | `github`, `china`, `google` (channel dimension) |

## Module Map

```
plain-app/
├── app/                          # Main application module
│   └── src/main/java/.../plain/
│       ├── MainApp.kt            # Application entry, init services
│       ├── MainActivity.kt       # Compose host activity (in ui/ too)
│       ├── Constants.kt          # Global constants
│       ├── TempData.kt           # Runtime ephemeral data
│       ├── SystemServices.kt     # Lazy system service accessors
│       │
│       ├── api/                  # HTTP client wrappers (3 files)
│       ├── chat/                 # P2P & group chat logic
│       ├── db/                   # Room entities, DAOs, migrations
│       ├── data/                 # Non-DB data models (40+ D*.kt)
│       ├── enums/                # 26 enum types
│       ├── events/               # Event bus events (50+ events)
│       ├── extensions/           # Kotlin extension functions
│       ├── features/             # Domain logic by feature area
│       ├── helpers/              # 33+ standalone utility helpers
│       ├── preferences/          # DataStore preferences (6 files)
│       ├── receivers/            # Broadcast receivers (5 files)
│       ├── services/             # Android services (7 files)
│       ├── ui/                   # ALL Compose UI code
│       ├── web/                  # HTTP server + GraphQL API
│       └── workers/              # WorkManager tasks
│
├── lib/                          # Shared utility library
│   └── src/main/java/.../lib/
│       ├── channel/              # Event bus infrastructure
│       ├── helpers/              # Crypto, JSON, Network, Search, SSL ...
│       ├── extensions/           # Kotlin extensions
│       ├── html2md/              # HTML → Markdown converter
│       ├── rss/                  # RSS feed parser
│       ├── upnp/                 # DLNA/UPnP support
│       ├── pdfviewer/            # PDF rendering
│       ├── logcat/               # Logging framework
│       └── ...                   # Other utilities
│
├── scripts/                      # Node.js i18n translation pipeline
├── docs/                         # Project documentation
└── gradle/libs.versions.toml     # Centralized dependency versions
```

## Source Code Packages (app module)

### `db/` — Database Layer (21 files)
Room database with 17 entities. Key files:
- `AppDatabase.kt` — `@Database` definition, entity list, schema version
- `Migrations.kt` — All migration specs (v1→v11)
- `D*.kt` — Entity classes: `DChat`, `DChatChannel`, `DNote`, `DFeed`, `DFeedEntry`, `DBook`, `DBookChapter`, `DTag`, `DTagRelation`, `DPeer`, `DSession`, `DBookmark`, `DAppFile`, `DPomodoroItem`

### `data/` — Data Models (40+ files)
Non-database data classes (API responses, UI models). Naming: `D*.kt` (DAudio, DImage, DVideo, DContact, DSim, DBattery, etc.).

### `enums/` — Enumerations (26 files)
`DataType` is the most important — maps to: AUDIO, IMAGE, VIDEO, NOTE, FEED_ENTRY, CONTACT, SMS, CALL, TAG, etc.

### `events/` — Event Bus (3 files, 50+ event classes)
- `AppEvents.kt` — UI & feature events (permissions, pairing, HTTP server state)
- `HttpApiEvents.kt` — Message CRUD events from web API
- `WebSocketEvents.kt` — WebSocket lifecycle events

### `features/` — Domain Logic (60+ files)
Organized by feature area:
| Directory | Scope |
|-----------|-------|
| `bluetooth/` | BLE scan, connect, read/write (5 files) |
| `book/` | e-book/PDF handling |
| `call/` | Call logs, SIM, blocked numbers |
| `contact/` | ContactsProvider CRUD |
| `feed/` | RSS feed fetch, parse, storage |
| `file/` | File system operations, storage stats |
| `locale/` | Language/locale handling |
| `media/` | MediaStore helpers (Audio/Image/Video/File/Call/Contact), DLNA cast |
| `sms/` | SMS/MMS read/write |
| Top-level | `AudioPlayer.kt`, `Permissions.kt`, `NoteHelper.kt`, `TagHelper.kt`, `PackageHelper.kt`, `BookmarkHelper.kt` |

### `helpers/` — Utilities (33+ files)
Stateless helper functions. Key ones:
- `AppHelper.kt` — App init/lifecycle
- `CryptoHelper.kt` → in `lib/helpers/` (search there for crypto)
- `DownloadHelper.kt`, `FileHashHelper.kt`, `PathHelper.kt` — File operations
- `NotificationHelper.kt` — Notification management
- `ShareHelper.kt`, `ScreenHelper.kt` — System integration
- `TimeAgoHelper.kt`, `FormatHelper.kt` — Formatting

### `preferences/` — Settings (6 files)
DataStore-based. Individual preferences defined as objects extending `BasePreference`. Key file: `Preferences.kt` aggregates 30+ preference reads.

### `services/` — Android Services (7+ files)
| Service | Role |
|---------|------|
| `HttpServerService.kt` | Foreground service running Ktor HTTP/WS server |
| `ScreenMirrorService.kt` | WebRTC screen mirroring |
| `AudioPlayerService.kt` | Media3 background audio playback |
| `PNotificationListenerService.kt` | System notification capture |
| `PlainAccessibilityService.kt` | Accessibility features |
| `QSTileService.kt` | Quick Settings tile |
| `webrtc/` | WebRTC peer session management |

### `web/` — HTTP Server & GraphQL (50+ files)
- `HttpServerManager.kt` — Ktor server setup, routes, TLS
- `MainGraphQL.kt` — Primary GraphQL schema (queries, mutations, subscriptions)
- `PeerGraphQL.kt` — Peer-to-peer communication schema
- `HttpModule.kt` — HTTP module configuration
- `models/` — 41 GraphQL response types (mirrors `data/` models for API)
- `loaders/` — Data loaders for GraphQL (Feeds, FileInfo, Tags, Mounts)
- `websocket/` — WebSocket helper, session management, WebRTC signaling

### `ui/` — Compose UI (100+ files)
```
ui/
├── base/              # 60+ reusable composables (P-prefixed: PAlert, PCard, PSwitch...)
│   └── coil/          # Image loading customization
├── components/        # 27+ domain-specific components
│   └── mediaviewer/   # Media viewer components
├── extensions/        # UI extension functions
├── helpers/           # UI helper functions
├── models/            # 38 ViewModels + data classes (VChat, VPackage, etc.)
├── nav/               # Navigation (Routing.kt defines all routes)
├── page/              # Feature screens (organized by feature)
│   ├── apps/          # App management (2 files)
│   ├── audio/         # Audio player & playlist (10 files with components/)
│   ├── cast/          # DLNA casting (3 files)
│   ├── chat/          # Chat screens (29 files with components/)
│   ├── docs/          # Document viewer (3 files)
│   ├── feeds/         # RSS feeds (10 files)
│   ├── files/         # File browser (7 files with components/)
│   ├── images/        # Image gallery (3 files)
│   ├── notes/         # Notes editor (4 files)
│   ├── pomodoro/      # Pomodoro timer (4 files)
│   ├── home/          # Home page & web portal (17 files)
│   ├── scan/          # QR/barcode scanner (4 files)
│   ├── settings/      # Settings screens (7 files)
│   ├── tags/          # Tag management (3 files)
│   ├── tools/         # Tools (1 file)
│   ├── videos/        # Video gallery (3 files)
│   └── web/           # Web server settings (7 files)
└── theme/             # Material3 theming (5 files, no subdirectories)
    # Theme.kt           — AppTheme composable (dark/light color schemes) + all color extensions
    # ColorHelper.kt      — Color utility functions (hex parsing, alwaysLight)
    # PlainTheme.kt       — Dimensions (margins, radius) + Typography extensions
    # Type.kt             — Material3 Typography definition
    # Shapes.kt           — Material3 Shapes definition
```

### `chat/` — Chat System (top-level, 10+ files)
- `ChatCacheManager.kt` — In-memory message cache
- `ChatDbHelper.kt` — DB operations for chat
- `PeerChatHelper.kt` — 1:1 peer chat logic
- `ChannelChatHelper.kt` — Group channel logic
- `PeerGraphQLClient.kt` — GraphQL client for peer communication
- `discover/` — mDNS/nearby device discovery
- `download/` — Chat file download management

## Key Patterns

### Naming Conventions
- **`D` prefix** → Data/DB class: `DChat`, `DAudio`, `DNote`
- **`V` prefix** → View data class: `VChat`, `VPackage`
- **`P` prefix** → UI composable: `PAlert`, `PCard`, `PSwitch`
- **`*Helper.kt`** → Stateless utility: `NoteHelper`, `TagHelper`
- **`*ViewModel.kt`** → ViewModel: `AudioViewModel`, `ChatViewModel`
- **`*Page.kt`** → Full-screen composable: `ChatPage`, `NotesPage`

### Data Flow
```
UI (Compose Page) → ViewModel (StateFlow) → Helper/Feature → DB (Room) / MediaStore / Network
                                           → sendEvent() for cross-component communication
```

### Event Bus Usage
```kotlin
// Send
sendEvent(HttpServerStateChangedEvent(state))
// Receive (in ViewModel or Composable)
receiveEvent<HttpServerStateChangedEvent> { event -> ... }
```

### GraphQL API Structure
Web clients connect to `https://device-ip:port/graphql`. Schema defined in `MainGraphQL.kt` using KGraphQL DSL:
```kotlin
query("notes") { resolver { ... } }
mutation("createNote") { resolver { ... } }
subscription("noteCreated") { ... }
```

## Resources (`app/src/main/res/`)

### String Files (split by feature)
| File | Content |
|------|---------|
| `strings_apps.xml` | App management strings |
| `strings_chat.xml` | Chat UI strings |
| `strings_common.xml` | Shared/common strings |
| `strings_feeds.xml` | RSS feed strings |
| `strings_files.xml` | File browser strings |
| `strings_media.xml` | Audio/video/image strings |
| `strings_network.xml` | Network/server strings |
| `strings_notes.xml` | Notes feature strings |
| `strings_permissions.xml` | Permission request strings |
| `strings_photo_exif.xml` | Photo EXIF metadata labels |
| `strings_settings.xml` | Settings screen strings |
| `strings_timeago.xml` | Time-ago formatting strings |
| `strings_tools.xml` | Tools feature strings |

### Locales (16 languages)
`values-zh-rCN`, `values-zh-rTW`, `values-de`, `values-fr`, `values-es`, `values-it`, `values-pt`, `values-ru`, `values-ja`, `values-ko`, `values-nl`, `values-tr`, `values-vi`, `values-hi`, `values-ta`, `values-bn`

## Build

```bash
./gradlew :app:assembleGithubDebug    # GitHub flavor debug
./gradlew :app:assembleGoogleRelease  # Google Play release
./gradlew :app:assembleChinaRelease   # China market release
./gradlew test                        # Unit tests
```

## AI Maintenance Shortcuts

```bash
scripts/check-ui-file-length.sh 150              # Verify UI file length guardrail
scripts/update-ui-file-length-violations.sh 150  # Sync docs/ui-file-length-violations.txt
```

## Key Dependencies (versions in `gradle/libs.versions.toml`)

| Category | Library | Usage |
|----------|---------|-------|
| UI | Jetpack Compose + Material3 | All screens |
| Images | Coil 3.4.0 | Image loading in Compose |
| Database | Room 2.8.4 | Local data persistence |
| Server | Ktor 3.4.1 | Embedded HTTP/WebSocket server |
| GraphQL | KGraphQL 0.19.0 | API schema |
| Media | Media3/ExoPlayer 1.9.2 | Audio/video playback |
| Camera | CameraX 1.5.3 | QR scanning |
| WebRTC | Stream WebRTC Android | Screen mirroring |
| Crypto | Tink + Bouncy Castle | E2E encryption |
| Markdown | Markwon 4.6.2 | Markdown rendering |
| HTTP | OkHttp 5.3.2 | HTTP client |
| Serialization | kotlinx-serialization | JSON encoding |
