# KMP Migration Plan

## Goal

Share as much UI and business logic as possible between the Android and iOS apps using Kotlin Multiplatform (KMP) + Compose Multiplatform (CMP).  The `shared/` Gradle module is the single source of truth for all cross-platform code.  Neither platform re-implements what the shared module already provides.

---

## Architecture

```
plain-app/
├── shared/              ← KMP module (Compose Multiplatform)
│   ├── commonMain/      ← ALL shared code: data, enums, UI, ViewModels, network
│   ├── androidMain/     ← Android-specific implementations (expect/actual)
│   └── iosMain/         ← iOS-specific implementations (expect/actual)
├── app/                 ← Android host; delegates to shared for shared features
└── iosApp/              ← iOS host (SwiftUI wrapper around shared Compose UI)
```

The `app/` module retains only Android-exclusive features (SMS, Contacts, DLNA, screen mirror, etc.).  The `iosApp/` module is a thin SwiftUI host that embeds the shared Compose UI.

---

## iOS Feature Scope

### iOS WILL support
| Feature | Notes |
|---|---|
| File management (browse, create, move, delete, rename) | Shared UI + local FileManager (iosMain) |
| Image & video gallery | Shared UI + PhotoKit bridge (iosMain) |
| Audio player | Shared UI + AVAudioPlayer bridge (iosMain) |
| Notes | Shared UI + DB |
| RSS Feeds | Shared UI + network |
| Tags | Shared UI + DB |
| Pomodoro timer | Shared UI |
| Settings | Shared UI (platform-specific items hidden per platform) |
| Peer chat | Shared UI + network |
| NAS file management (SFTP/SMB/WebDAV) | Future; iosMain implementation |

### iOS WILL NOT support (Android-only features)
| Feature | Android Implementation | iOS plan |
|---|---|---|
| SMS management | `SmsManager` + `ContentResolver` | Not needed |
| Contact management | `ContactsContract` | Not needed |
| Installed app management | `PackageManager` | Not needed |
| Bluetooth device management | `BluetoothAdapter` | Not needed |
| DLNA cast / receiver | `nanodlna` library | Not needed |
| Screen mirror (WebRTC) | `libwebrtc` + `MediaProjection` | Not needed |
| Blocked numbers | `BlockedNumberContract` | Not needed |
| Call log | `CallLog.Calls` | Not needed |
| Device / SIM info | `TelephonyManager` | Not needed |
| Android notification management | `NotificationManager` | Not needed |
| QR scanner (camera) | `MLKit` | Possible future via AVFoundation |

---

## Migration Phases

### Phase 0: Foundation — ✅ COMPLETED (2025-05-21)

| Item | Status | Location |
|---|---|---|
| `lifecycle-viewmodel-compose:2.9.5` added | ✅ | `shared/build.gradle.kts` |
| `kotlinx-datetime:0.7.1` added | ✅ | `shared/build.gradle.kts` |
| Data classes (25 files) | ✅ | `shared/commonMain/data/` |
| Enums (21 files) | ✅ | `shared/commonMain/enums/` |
| Extensions (3 files: Boolean, StringEpochMillis, StringFileSize) | ✅ | `shared/commonMain/extensions/` |
| `TimeHelper` (fixed `System.currentTimeMillis()` → `Clock.System.now()`) | ✅ | `shared/commonMain/helpers/` |
| ViewModel base: `ISearchableViewModel`, `ISelectableViewModel` | ✅ | `shared/commonMain/ui/models/` |
| `UpdateViewModel`, `FolderOption`, `VTabData`, `VClickText` | ✅ | `shared/commonMain/ui/models/` |
| `PomodoroState` | ✅ | `shared/commonMain/ui/page/pomodoro/` |

---

### Phase 1: Theme & Design System — ✅ COMPLETED (2025-05-21)

Move the entire `ui/theme/` package to `shared/commonMain`.  None of these files import `android.*` — they are pure Compose Multiplatform.

**Steps:**
1. Extract `LocalDarkTheme` / `LocalAmoledDarkTheme` CompositionLocals to `shared/commonMain/preferences/LocalPreferences.kt` (removes the Android dependency from Theme.kt).
2. Move `Shapes.kt` → `shared/commonMain/ui/theme/`
3. Move `Type.kt` → `shared/commonMain/ui/theme/`
4. Fix `ColorHelper.kt` (`java.lang.Long.parseLong` → `toLong(16)`) and move to `shared/commonMain/ui/theme/`
5. Move `Theme.kt` → `shared/commonMain/ui/theme/`
6. Move `PlainTheme.kt` → `shared/commonMain/ui/theme/`
7. Move `ButtonType.kt` → `shared/commonMain/enums/` (now unblocked: depends on `ColorScheme.red`)
8. Remove duplicate `LocalDarkTheme` / `LocalAmoledDarkTheme` from `app/.../preferences/Settings.kt`

| File | Status |
|---|---|
| `preferences/LocalPreferences.kt` (new, commonMain) | ✅ |
| `ui/theme/Shapes.kt` | ✅ |
| `ui/theme/Type.kt` | ✅ |
| `ui/theme/ColorHelper.kt` | ✅ |
| `ui/theme/Theme.kt` | ✅ |
| `ui/theme/PlainTheme.kt` | ✅ |
| `enums/ButtonType.kt` | ✅ |

---

### Phase 2: Multiplatform Preferences — ✅ COMPLETED (2025-07)

**What was done:**
1. Added `androidx.datastore:datastore-preferences-core:1.2.1` to `shared/commonMain` dependencies (KMP-compatible variant).
2. Added `kotlinx-serialization-json:1.11.0` to shared for JSON serialization of complex preferences.
3. Added `kotlin-serialization` plugin alias to `gradle/libs.versions.toml` and `shared/build.gradle.kts`.
4. Created `shared/commonMain/preferences/AppDataStore.kt` — global `DataStore<Preferences>` singleton (`appDataStore`) initialized via `initDataStore(store)`. No `Context` required.
5. Created `shared/commonMain/preferences/DataStoreExt.kt` — `DataStore<Preferences>.put(key, value)` and `getAsync(key)` helpers using `updateData` (KMP-compatible, avoids Java-only `edit`).
6. Created `shared/commonMain/preferences/BasePreference.kt` — abstract base class with `get(Preferences)`, `getAsync()`, `putAsync(value)` — all Context-free.
7. Created `shared/commonMain/preferences/Preferences.kt` — migrated 60+ preference objects to shared. Objects requiring Android APIs (DarkTheme, DeviceName, Language, Web, Https, etc.) have only `key`/`default`/`get` in shared; Android side-effects remain in `app/preferences/AndroidPreferences.kt`.
8. Kept in `app/preferences/Preferences.kt` (Android-specific only): `BaseSortByPreference` + 6 sort-by subclasses (depend on `FileSortBy` → `android.provider.MediaStore`), `AudioPlaylistPreference` (depends on `DPlaylistAudio` → `Parcelable`), `HomeFeaturesPreference` and `HomeSectionCollapsedPreference` (depend on `AppFeatureType`).
9. Created `app/preferences/AndroidPreferences.kt` — Android-specific extension functions for preferences with side effects (DarkTheme, DeviceName, Language, Web, Https, ApiPermissions, AdbToken, UrlToken, ClientId, KeyStorePassword, MdnsHostname).
10. `app/preferences/DlnaCastRulesPreference.kt` — cleared (objects moved to shared).
11. `app/preferences/SignatureKeyPreference.kt` — replaced object definition with extension functions (object now in shared).
12. `app/MainApp.kt` — added `initDataStore(dataStore)` call at startup; changed `dataStore.getPreferencesAsync()` to top-level `getPreferencesAsync()`.
13. Fixed KMP incompatibilities: replaced `MutableList.removeIf` (Java-only) with `removeAll`; changed `TimeHelper.kt` to use `kotlin.time.Clock` instead of `kotlinx.datetime.Clock`.
14. Both `:shared:compileCommonMainKotlinMetadata` and `:app:compileDebugKotlin` build cleanly.

**Remaining for full iOS support:** Implement iOS `DataStore` initialization (requires `okio` path) in `iosMain`.

---

### Phase 3: Networking (Ktor) — ⏳ TODO

`HttpClientManager.kt` already uses Ktor but imports `android.util.Base64` and `com.ismartcoding.lib.*`.

**Steps:**
1. Replace `android.util.Base64` with `kotlin.io.encoding.Base64` (stdlib, commonMain since Kotlin 1.8).
2. Replace `com.ismartcoding.lib.helpers.CryptoHelper` with a multiplatform crypto solution (`kotlinx-crypto` or `ktor-crypto`).  Or, for HMAC/AES, use `platform.darwin.*` (iosMain) and `javax.crypto.*` (androidMain) behind an `expect/actual`.
3. Add Ktor dependencies to `shared/build.gradle.kts` (client-core + CIO/Darwin engines).
4. Move `api/HttpClientManager.kt`, `api/ApiResult.kt`, `api/HttpApiTimeout.kt` to `shared/commonMain/api/`.

**Blocked by:** `com.ismartcoding.lib.helpers.CryptoHelper` (Android-only lib).  Needs an `expect/actual` crypto wrapper.

---

### Phase 4: Database Layer — ⏳ TODO

The current database uses Room (Android-only).  Two options:

| Option | Pros | Cons |
|---|---|---|
| **KMP Room** (1.0.0-alpha+) | Same API as existing Room, minimal rewrite | Still in alpha for KMP; iOS support via SQLite |
| **SQLDelight** | Mature KMP-first, generates type-safe Kotlin | Requires schema rewrite from Room annotations |

**Recommendation:** Migrate to **KMP Room** first (least disruption), then evaluate SQLDelight for new tables.

**Steps:**
1. Add `androidx.room:room-runtime` KMP dependency to `shared/build.gradle.kts`.
2. Add `room-compiler` KSP processor to all targets.
3. Move `db/` entities and DAOs to `shared/commonMain/db/` one by one, verifying each compiles.
4. Create `expect fun createDatabase(driver: RoomDatabase.Builder<AppDatabase>): AppDatabase` with `actual` per platform.
5. Remove Room from `app/build.gradle.kts` (now provided by shared).

**Blocked by:** KMP Room alpha maturity.  Evaluate before committing.

---

### Phase 5: ViewModels — ⏳ TODO (blocked by Phase 2 + Phase 4)

Most ViewModels are blocked by:
- Room DB entities (`com.ismartcoding.plain.db.*`)
- Android features (`com.ismartcoding.plain.features.*`)
- Android DataStore preferences

After Phase 2 (Preferences) and Phase 4 (DB) complete, ViewModel migration is unblocked:

Priority order:
1. `NotesViewModel` — depends only on DB + preferences (no Android features)
2. `FeedsViewModel` — depends on DB + network
3. `AudioViewModel`, `ImagesViewModel`, `VideosViewModel` — depends on DB + media helpers (need `expect/actual` for MediaStore vs PhotoKit)
4. `FilesViewModel` — depends on `FileSystemHelper` (need `expect/actual` for file access)
5. `ChatViewModel` — depends on DB + network

---

### Phase 6: Shared Feature UIs — ⏳ TODO (blocked by Phase 5)

Once ViewModels are in commonMain, move the Composable screen files:

| Feature | Pages | Priority |
|---|---|---|
| Files | `ui/page/files/` (15 files) | High (iOS NAS use case) |
| Images | `ui/page/images/` (7 files) | High |
| Videos | `ui/page/videos/` (7 files) | High |
| Audio | `ui/page/audio/` (22 files) | Medium |
| Notes | `ui/page/notes/` (7 files) | Medium |
| Feeds | `ui/page/feeds/` (15 files) | Medium |
| Tags | `ui/page/tags/` (3 files) | Medium |
| Pomodoro | `ui/page/pomodoro/` (7 files) | Low |
| Settings | `ui/page/settings/` (9 files) | High (different content per platform) |
| Chat | `ui/page/chat/` (46 files) | Medium |
| Home | `ui/page/home/` (20 files) | High (entry point) |

**Note:** Settings page should use `expect/actual` to show platform-specific items.

---

### Phase 7: Navigation — ⏳ TODO

Implement shared navigation with **Compose Navigation for KMP** (`androidx.navigation:navigation-compose`).

**Steps:**
1. Add `androidx.navigation:navigation-compose` (KMP version) to `shared/build.gradle.kts`.
2. Define shared route constants in `shared/commonMain/ui/nav/`.
3. Create `SharedAppNavHost` composable in commonMain.
4. Android and iOS host apps initialize `SharedAppNavHost` directly.

---

### Phase 8: Platform-Specific Implementations (expect/actual) — ⏳ ONGOING

Patterns used across phases for platform-specific code:

#### File System Access
```kotlin
// commonMain
expect fun platformFileSystem(): FileSystem  // or an abstraction

// androidMain
actual fun platformFileSystem() = FileSystem.SYSTEM  // okio

// iosMain
actual fun platformFileSystem() = FileSystem.SYSTEM  // okio (NSFileManager backed)
```

#### Media / Photo Library
```kotlin
// commonMain
expect suspend fun getImages(bucketId: String?): List<DImage>

// androidMain
actual suspend fun getImages(...) = ImageMediaStoreHelper.getImages(...)  // MediaStore

// iosMain
actual suspend fun getImages(...) = PHPhotoLibrary ...  // PhotoKit via Kotlin/Native
```

#### Cryptography
```kotlin
// commonMain
expect fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray

// androidMain
actual fun hmacSha256(...) = javax.crypto.Mac.getInstance("HmacSHA256")...

// iosMain
actual fun hmacSha256(...) = CommonCrypto (platform.darwin.*)
```

#### DataStore Path
```kotlin
// androidMain
actual fun dataStoreFile() = context.filesDir.resolve("settings.preferences_pb")

// iosMain
actual fun dataStoreFile() = File(NSHomeDirectory(), "settings.preferences_pb")
```

---

## Dependency Additions Needed

Add to `shared/build.gradle.kts` commonMain as phases progress:

| Dependency | Phase | Purpose |
|---|---|---|
| `androidx.datastore:datastore-preferences-core` | Phase 2 | Multiplatform preferences |
| `io.ktor:ktor-client-core` | Phase 3 | HTTP client (already in app, move to shared) |
| `io.ktor:ktor-client-cio` | Phase 3 | JVM/Android engine |
| `io.ktor:ktor-client-darwin` | Phase 3 | iOS engine |
| `androidx.room:room-runtime` (KMP) | Phase 4 | Database |
| `androidx.navigation:navigation-compose` (KMP) | Phase 7 | Navigation |

---

## What Stays Android-Only (in `app/`)

The following will never go to `shared/commonMain`.  They live permanently in `app/src/main/`:

| Package | Reason |
|---|---|
| `features/sms/` | `android.telephony.SmsManager` |
| `features/contact/` | `android.provider.ContactsContract` |
| `features/call/` | `android.provider.CallLog`, `BlockedNumberContract` |
| `features/bluetooth/` | `android.bluetooth.*` |
| `features/dlna/` | Android DLNA library |
| `services/webrtc/` | `MediaProjection` + `libwebrtc` |
| `receivers/` | `BroadcastReceiver` |
| `services/` | Android `Service`, `ForegroundService` |
| `workers/` | Android `WorkManager` |
| `web/` | Ktor server (HTTP server for web UI) — Android-specific use case |
| `mdns/` | Android `NsdManager` |

---

## Progress Log

| Date | Phase | Action |
|---|---|---|
| 2025-05-21 | Phase 0 | Added lifecycle-viewmodel-compose + kotlinx-datetime deps |
| 2025-05-21 | Phase 0 | Migrated 25 data classes, 21 enums, 3 extensions, TimeHelper, 6 ViewModel files, PomodoroState |
| 2025-05-21 | Phase 1 | Migrated entire ui/theme/ system + ButtonType to shared/commonMain |
