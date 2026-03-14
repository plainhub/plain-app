# Copilot Instructions for PlainApp

> **Start here**: Read `docs/ARCHITECTURE.md` for full project structure and module map.
> Avoid scanning/searching the codebase blindly — use the directory guide below to jump directly to the right package.

## Project Overview

PlainApp is an Android app (Kotlin + Jetpack Compose) that provides web-based phone management via a local network. It runs an embedded Ktor HTTP server exposing a GraphQL API, with E2E encryption (XChaCha20-Poly1305) and WebRTC screen mirroring.

- **Two modules**: `:app` (main) and `:lib` (shared utilities)
- **No DI framework** — uses companion objects, singletons, manual construction
- **Event bus** — `sendEvent()` / `receiveEvent<T>` from `lib/channel/`
- **Package root**: `com.ismartcoding.plain` (app) / `com.ismartcoding.lib` (lib)

## Where to Find Things

| What you need | Where to look |
|---------------|---------------|
| DB entities & DAOs | `app/.../plain/db/` (D-prefixed: `DChat.kt`, `DNote.kt`, `DFeed.kt` ...) |
| Non-DB data models | `app/.../plain/data/` (D-prefixed: `DAudio.kt`, `DImage.kt` ...) |
| Enum types | `app/.../plain/enums/` (26 files, key one: `DataType.kt`) |
| Event definitions | `app/.../plain/events/AppEvents.kt` (50+ events in one file) |
| Domain logic by feature | `app/.../plain/features/{bluetooth,book,call,contact,feed,file,media,sms}/` |
| Utility helpers | `app/.../plain/helpers/` (33+ stateless helper files) |
| Preferences/settings | `app/.../plain/preferences/` (6 files, DataStore-based) |
| Android services | `app/.../plain/services/` (HTTP server, screen mirror, audio, notifications) |
| ViewModels | `app/.../plain/ui/models/` (38 ViewModels) |
| UI screens/pages | `app/.../plain/ui/page/{feature}/` (organized by feature) |
| Reusable composables | `app/.../plain/ui/base/` (P-prefixed: `PAlert`, `PCard`, `PSwitch` ...) |
| Navigation routes | `app/.../plain/ui/nav/Routing.kt` |
| GraphQL schema | `app/.../plain/web/MainGraphQL.kt` (primary), `PeerGraphQL.kt` (peer) |
| GraphQL response models | `app/.../plain/web/models/` (41 types) |
| HTTP server setup | `app/.../plain/web/HttpServerManager.kt` |
| WebSocket handling | `app/.../plain/web/websocket/` |
| Chat system | `app/.../plain/chat/` (cache, DB helper, peer/channel logic, discovery) |
| Broadcast receivers | `app/.../plain/receivers/` (5 files) |
| Workers | `app/.../plain/workers/FeedFetchWorker.kt` |
| Lib crypto/network | `lib/.../lib/helpers/CryptoHelper.kt`, `NetworkHelper.kt`, `SslHelper.kt` |
| Lib event bus | `lib/.../lib/channel/` |
| App entry point | `app/.../plain/MainApp.kt` (Application), `ui/MainActivity.kt` (Activity) |
| Build config & deps | `app/build.gradle.kts`, `gradle/libs.versions.toml` |
| String resources | `app/src/main/res/values/strings_{feature}.xml` (split by feature) |
| Localized strings | `app/src/main/res/values-{locale}/strings_{feature}.xml` |

> **Short path notation**: `app/.../plain/` = `app/src/main/java/com/ismartcoding/plain/`  
> **Lib short path**: `lib/.../lib/` = `lib/src/main/java/com/ismartcoding/lib/`

## Naming Conventions

| Prefix/Suffix | Meaning | Example |
|---------------|---------|---------|
| `D` prefix | Data/DB entity class | `DChat`, `DAudio`, `DNote` |
| `V` prefix | View data class (UI-specific) | `VChat`, `VPackage` |
| `P` prefix | Reusable Compose component | `PAlert`, `PCard`, `PSwitch` |
| `*Helper.kt` | Stateless utility | `NoteHelper`, `TagHelper`, `PathHelper` |
| `*ViewModel.kt` | ViewModel | `ChatViewModel`, `AudioViewModel` |
| `*Page.kt` | Full screen composable | `ChatPage`, `NotesPage`, `FeedsPage` |
| `*BottomSheet.kt` | Bottom sheet composable | `ViewNoteBottomSheet` |
| `*Dialog.kt` | Dialog composable | `CastDialog`, `UpdateDialog` |

## Architecture Pattern

```
Compose Page → ViewModel (StateFlow) → Helper/Feature → Room DB / MediaStore / Network
                                      ↕ Event bus (sendEvent/receiveEvent)
```

- **No Hilt/Dagger** — services accessed via `SystemServices.kt` or companion objects
- **State**: `MutableStateFlow` in ViewModels, collected by Compose
- **Side effects**: coroutine-based (`coIO`, `coMain` from `lib/helpers/CoroutinesHelper.kt`)

## Build Commands

```bash
./gradlew :app:assembleGithubDebug     # GitHub debug APK
./gradlew :app:assembleGoogleRelease   # Google Play release
./gradlew :app:assembleChinaRelease    # China market release
./gradlew test                         # Unit tests
```

Flavors: `github` / `china` / `google` (channel dimension). See `app/build.gradle.kts`.

## i18n Translation Workflow

### When to use
Run this workflow any time new strings are added to `app/src/main/res/values/strings.xml`, or when you suspect other locales have untranslated (still-English) strings.

### How to trigger
Tell Copilot:
> **"同步翻译"** or **"sync i18n translations"** or **"检查并补全多语言翻译"**

Copilot will run the three-step pipeline below **from the `plain-app` project root**.

### Three-step pipeline

```bash
# Step 1 – detect missing keys and untranslated (English) values
node scripts/i18n-find-untranslated.mjs
# → writes scripts/i18n-todo.json  (grouped by locale folder, e.g. values-de)

# Step 2 – translate only the affected keys via Google Translate (free, no API key)
node scripts/i18n-translate-todo.mjs
# → writes scripts/i18n-translated.json
# → writes scripts/i18n-stable.json  (loanwords / brand names intentionally same as English)

# Step 3 – apply translations back into each locale strings.xml
node scripts/i18n-apply-todo.mjs

# Verify clean
node scripts/i18n-find-untranslated.mjs
# → should print "Total: 0 missing, 0 untranslated"
```

### Key design decisions
- Base locale is `app/src/main/res/values/strings.xml` (English).
- Only the **delta** (missing/untranslated keys) is sent for translation — never the whole file.
- Android-style placeholders (`%1$s`, `%s`, `{{path}}`) are protected before translation and restored afterwards.
- Keys where Google Translate returns the same value as English (loanwords, brand names, tech terms) are recorded in `scripts/i18n-stable.json` and skipped in future runs.
- Intermediate files (`i18n-todo.json`, `i18n-translated.json`) are gitignored; `i18n-stable.json` **must be committed**.

### Scripts location
| Script | Purpose |
|--------|---------|
| `scripts/i18n-find-untranslated.mjs` | Detect missing / English-value keys → `i18n-todo.json` |
| `scripts/i18n-translate-todo.mjs` | Translate via Google Translate → `i18n-translated.json` |
| `scripts/i18n-apply-todo.mjs` | Patch locale `strings.xml` files |
| `scripts/i18n-stable.json` | Cache of keys correctly staying as English (auto-managed, commit this) |

### Locale directory mapping
| Folder | Language |
|--------|----------|
| `values-zh-rCN` | Chinese Simplified |
| `values-zh-rTW` | Chinese Traditional |
| `values-de` | German |
| `values-fr` | French |
| `values-es` | Spanish |
| `values-it` | Italian |
| `values-pt` | Portuguese |
| `values-ru` | Russian |
| `values-ja` | Japanese |
| `values-ko` | Korean |
| `values-nl` | Dutch |
| `values-tr` | Turkish |
| `values-vi` | Vietnamese |
| `values-hi` | Hindi |
| `values-ta` | Tamil |
| `values-bn` | Bengali |
