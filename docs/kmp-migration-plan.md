# KMP Migration Plan

> 目标：把 `app/` 里的业务代码大部分搬到 `shared/`；`app/` 只留 Android 壳（Activity/Service/Receiver/Manifest），`iosApp/` 已是 SwiftUI 壳，shared 暴露 Compose UI 供两端复用。
> 节奏：小步快进。每一步 = 一次 `./gradlew :shared:compileCommonMainKotlinMetadata` + `:app:assembleDebug` 通过 + 一个 commit。

---

## 1. 标准 KMP 目录结构（最终目标）

> **iOS 端最终目标**：只保留 `PlainIOSApp.swift` + `ContentView.swift` 这 2 个 SwiftUI host 文件，**复用整个 shared/commonMain/ui/ Compose UI**（Android 和 iOS 同一套界面），Android 特有功能（蓝牙/SMS/DLNA/通话/通知监听/WebRTC）通过 `expect/actual fun isAndroidOnlyFeatureEnabled(): Boolean` 在 shared 顶层判断隐藏。iOS 端删掉 `PlainHomeViewController` 这类 demo 的 iosMain 旧桥接，改用 `SharedAppNavHost` 嵌入。

```
plain-app/
├── shared/                ← KMP 单源（Compose Multiplatform + Room + Ktor）
│   ├── src/commonMain/    ← 业务核心：data/enums/db/helpers/extensions/ui/preferences/i18n + 整个 Compose UI
│   ├── src/androidMain/   ← Android-only actual（CameraX/MediaStore/Tink/Filesystem + setAppContext + currentLocale）
│   ├── src/iosMain/       ← iOS-only actual（PhotoKit/AVAudioPlayer/NSFileManager + currentLocale → NSLocale.languageCode）
│   ├── src/desktopMain/   ← 暂不启用，保留目录待以后
│   ├── src/jvmMain/       ← 暂不启用
│   └── schemas/           ← Room schema export
├── app/                   ← Android 入口壳（瘦模块）
│   ├── src/main/AndroidManifest.xml
│   ├── src/main/java/.../MainApp.kt        ← 仅 Application 初始化 + SharedAppNavHost 桥
│   ├── src/main/java/.../MainActivity.kt   ← 仅 Activity + setContent { PlainTheme { SharedAppNavHost() } }
│   ├── src/main/java/.../features/{sms,contact,call,bluetooth,dlna}  ← Android-only 永远不迁
│   ├── src/main/java/.../services/webrtc  ← 永远不迁
│   ├── src/main/java/.../{receivers,workers,mdns,web}                ← Android-only 永远不迁
│   └── src/main/res/      ← Android-only 资源（launcher icon / values-night / fdroid 资源）
└── iosApp/                ← iOS 入口壳（极薄 SwiftUI host）
    └── iosApp/{PlainIOSApp,ContentView}.swift + Info.plist
```

**关于 `app/` 是否重命名 `androidApp/`：不做**。理由：
- 当前 `:app` Gradle 模块名、`com.ismartcoding.plain` Kotlin 包、`com.ismartcoding.plain` Android applicationId 已稳定，改名 = 改 ~1321 个文件的 import 路径 + 改 AndroidManifest applicationId + 改 release 签名匹配 + 改 CI 脚本 + 破 git blame。重写成本远高于收益。
- KMP 模板默认 `composeApp/` 适用于「零起步」项目；本项目是「Android-first → 抽 KMP」，逆向命名 `composeApp` 没意义。

---

## 2. Snapshot 2026-06-26

| 模块 | kt 数 | 说明 |
|---|---|---|
| `app/src/main` | 1321 | 业务大头（chat/ui/api/features/services/db 包都在这） |
| `shared/commonMain` | 99 | 已迁：data(25)、db(24 实体+1 AppDatabase)、enums、helpers(7)、extensions(3)、preferences(60+)、ui/{theme,page,models} |
| `shared/androidMain` | 1 | PlatformDispatchers.kt（kotlin.time 适配） |
| `shared/iosMain` | 3 | AppDatabaseFactory.ios.kt + PlatformDispatchers.kt + PlainHomeViewController.kt |
| `shared/desktopMain` | 0 | 空目录，未启用 |

**已完成（Phase 0~2）**：
- [x] Phase 0 — lifecycle/datetime 依赖 + data/enums/extensions/TimeHelper/PomodoroState
- [x] Phase 1 — ui/theme 整体 + ButtonType
- [x] Phase 2 — DataStore 跨平台 + 60+ preference 迁移到 shared

**进行中**：
- [x] Phase 4 大部分 — Room 实体 + AppDatabase 已迁；Android 端剩 Migrations/DataInitializer/RawQueryHelper/扩展函数
- [x] Phase 6 部分 — 已有 shared/ui/page/{files,notes,feeds,...}、shared/shared/{home}、iOS 入口已能跑 PlainHome

**完全未做**：
- [ ] Phase 3 — Ktor `app/api/{HttpClientManager,ApiResult,HttpApiTimeout}` 还在 app
- [ ] Phase 7 — `app/ui/nav/` 还在 app，未用 compose multiplatform navigation
- [ ] Phase 9 — `app/` 还没瘦下来（1321 个文件，体量与目标相差大）

---

## 3. Phases（剩下要做的事）

> 每条 checkbox = 一次 build pass + 一次 commit。改完跑：
> ```
> ./gradlew :shared:compileCommonMainKotlinMetadata :shared:compileDebugKotlinAndroid :app:assembleDebug
> ```
> 全绿才能翻 `[x]`。

### Phase 3 — Networking（Ktor 迁到 shared）

> 现状修正（2026-06-26）：`HttpClientManager` 不是纯 Ktor，还混合了 OkHttp + CryptoHelper + MainApp + PhoneHelper + Android-only HttpResponse.isOk 扩展。**不能整文件搬**。先拆 Ktor-only 部分到 shared，OkHttp + crypto 部分留 app（用 expect/actual 逐步解）。

- [x] 3.1 在 `shared/commonMain` 加 ktor-client-core + androidMain 加 ktor-client-cio/logging + iosMain 加 ktor-client-darwin。<!-- 2026-06-26: Android `:app:assembleDebug` 全绿（github/google/fdroid 三 flavor）；iOS `:shared:compileKotlinIosArm64` 仍被预存在 KSP+Compose Resources blocker 卡住，见 Phase 12 -->
- [x] 3.2 把 `app/api/HttpApiTimeout.kt` 原样搬到 `shared/commonMain/api/HttpApiTimeout.kt`，删 app 里那份（package `com.ismartcoding.plain.api` 保持不变，import 不变）。<!-- 2026-06-26: `:app:assembleDebug` 全绿，import 路径不变 -->
- [x] 3.3 把 `app/api/ApiResult.kt` 搬到 `shared/commonMain/api/ApiResult.kt`。先解决两个依赖：① `HttpResponse.isOk()` 扩展（当前在 `app/lib/extensions/HttpResponse.kt`）→ 复制一份到 `shared/commonMain/api/HttpResponseExt.kt`；② `LocaleHelper.getString(Res.string.unknown)` → 用 expect/actual `appStringResource(id)`，iosMain actual 返回字符串字面量<!-- 2026-06-26: 实操简化 — 直接 inline `runBlocking { getComposeString(Res.string.unknown) }`，省去 expect/actual 复杂度（iOS 死锁风险记 Phase 8.5）。`:app:assembleDebug` 全绿 -->
- [x] 3.4 把 `app/api/HttpClientManager.kt` 拆成两个 object：
  - `KtorClientFactory`（shared/commonMain）— `httpClient()` 和 `browserClient()`，CIO/Darwin engine 自适应
  - `OkHttpClientFactory`（app/api）— `downloadClient()`/`createCryptoHttpClient()`/`createUnsafeOkHttpClient()`<!-- 2026-06-26: 拆完。KtorClientFactory 用 object + internal expect fun 模式（browserClient 是 platformBrowserClient() 的 wrapper）。Logging plugin 是 androidMain-only，iosMain 暂不装。引入 HttpLogSink（fun interface + global var）作为 Logging 抽象，app 在 Phase 3.7 注入 LogCat。`:app:assembleDebug` 全绿。改了 15 个引用方 import。-->
- [x] 3.5 解 `CryptoHelper.chaCha20Encrypt/Decrypt` Android-only 依赖：在 shared 加 `expect fun chaCha20Encrypt(key, plaintext): ByteArray` + actual，androidMain 用 Tink 或 javax.crypto，iosMain 用 platform.darwin.CommonCrypto（或 CryptoKit）<!-- 2026-06-26: N/A — `CryptoHelper` 强绑 `com.google.crypto.tink.*` + `android.util.Base64` + `java.nio.file.*`，整体 Android-only；Phase 3 目标只是「Ktor 共享」，`OkHttpClientFactory.createCryptoHttpClient()` 已留 app 不影响目标。后续若要 iOS 支持 ECDH chat 加密再单独立 phase -->
- [x] 3.6 解 `NetworkHelper.isLocalNetworkAddress` Android-only：在 shared 加 `expect fun isLocalNetworkAddress(host: String): Boolean`，androidMain 保留原实现，iosMain 用 platform.darwin POSIX<!-- 2026-06-26: N/A — 同 3.5。`OkHttpClientFactory.createUnsafeOkHttpClient()` 已留 app，NetworkHelper 仅 OkHttp 用，不影响 Ktor 共享 -->
- [x] 3.7 解 `LogCat` Android-only：`browserClient()` 的 `Logging` plugin 用一个 shared 的 `Logger` 实现，把 LogCat 注入延后到 `MainApp.onCreate`（Android-only）或保留为空 iOS no-op<!-- 2026-06-26: 实现 — `shared/commonMain/api/HttpLogSink.kt` 用 fun interface + global var 提供 `httpLogSink: HttpLogSink`，default `println("[HttpLog] $it")`。`shared/androidMain/api/KtorClientFactory.android.kt:18-21` 把 Ktor `Logger` 包成 `httpLogSink.log(message)`。`app/MainApp.kt:94` 在 `onCreate` 里覆盖 `httpLogSink = HttpLogSink { LogCat.v(it) }`。iOS 暂不装 Logging plugin（KtorClientFactory.ios.kt），与 plan 描述一致。`:app:assembleDebug` 全绿 -->
- [x] 3.8 `app/build.gradle.kts` 删除 ktor-client-core / logging 的直接依赖（core 已由 shared api 透传；logging 由 shared 提供）；保留 `ktor-client-cio`（app 自己 3 个 DLNA 文件直接 import `io.ktor.client.engine.cio.CIO`）。<!-- 2026-06-26: 调整 — `ktor-client-cio` 不能删（DLNA/Cast 3 个文件用）。`ktor-client-core` 删（shared api 透传）。`ktor-client-logging` 删（shared androidMain 提供）。build 全绿 -->
- [x] 3.9 `./gradlew :app:assembleDebug` 全绿（github/google/fdroid 三 flavor），`import com.ismartcoding.plain.api.*` 路径不变<!-- 2026-06-26: 全绿 -->
- [ ] 3.10 commit：`chore(kmp): move ktor client to shared commonMain`<!-- 2026-06-26: 用户暂不 commit，保留待批 -->

### Phase 12 — Pre-existing Blockers

- [ ] 12.1 iOS `:shared:compileKotlinIosArm64` 失败：`kspKotlinIosArm64` 找不到 `build/generated/compose/resourceGenerator/kotlin/commonMainResourceAccessors/com/ismartcoding/plain/i18n/String3.commonMain.kt`（KSP + Compose Resources race condition）。**预存在，与本次 KMP 改造无关**。验证：`git stash` 后同样失败。
  - 影响范围：所有 iOS KMP 编译 task
  - 不阻塞 Phase 3~11 中纯 Android 的步骤
  - 待修时再起一个独立 phase，或交给 compose-multiplatform 上游

> 注：Phase 3 不包括 `web/`（Ktor HTTP server）—— 那块永远在 `app/`，见底部「Android-Only」清单。

### Phase 4 — Database 收尾

- [x] 4.1 把 `app/db/Migrations.kt` 搬到 `shared/androidMain/db/Migrations.kt`（注意：`SupportSQLiteDatabase` 是 androidMain-only interface，**不能放 commonMain**）<!-- 2026-06-26: 调整 — Migration 必须在 androidMain。`:app:assembleDebug` 全绿 -->
- [x] 4.2 把 `app/db/RawQueryHelper.kt` 搬到 `shared/commonMain/db/RawQueryHelper.kt`<!-- 2026-06-26: 全绿 -->
- [x] 4.3 把 `app/db/DataInitializer.kt` 搬到 `shared/commonMain/db/DataInitializer.kt`（它依赖 Application Context？看是否需要改为构造时注入 `Context?` 参数，androidMain 传 Context，iosMain 传 null）<!-- 2026-06-26: N/A — `DataInitializer` 用 `android.content.ContentValues` + `android.database.sqlite.SQLiteDatabase.CONFLICT_NONE` + `LocaleHelper.getString` Android-only + `StringHelper.shortUUID`/`String.cut` Android-only，留 app。后续要 iOS 支持初始数据再单独 phase -->
- [x] 4.4 `app/db/{DChatChannelExtensions,DPeerExtensions,DChatExtensions}.kt` 检查：若只是 DAO 包装，按需搬到 `shared/commonMain/db/`，**不动 shared 已有同名 entity 文件**<!-- 2026-06-26: N/A — 三个文件都强依赖 `CryptoHelper`/`NetworkHelper`/`PeerCacher`/`TempData`/`Base64`/`Context`/`LocaleHelper` Android-only，留 app。Phase 5 ViewModel 迁移时再考虑是否需要 expect/actual 化 -->
- [x] 4.5 `MainApp.kt` 改用 `initDatabase(Room.databaseBuilder(...))` 但 builder 来自 shared 的 `expect fun appDatabaseBuilder(name: String): RoomDatabase.Builder<AppDatabase>`，androidMain actual 用 `applicationContext`<!-- 2026-06-26: 实现 — `shared/commonMain/db/AppDatabase.kt` 加 `expect fun buildAppDatabase(name: String): RoomDatabase.Builder<AppDatabase>`；androidMain 用 `setAppContext(context)` + `Room.databaseBuilder(ctx, name).addMigrations(Migrations.MIGRATION_5_6)`；iosMain 复用 `NSDocumentDirectory/$name` + `BundledSQLiteDriver`。MainApp.onCreate 加 `setAppContext(this)` + 改用 `buildAppDatabase(Constants.DATABASE_NAME)`。全绿 -->
- [x] 4.6 `app/src/main/java/com/ismartcoding/plain/db/` 只剩 Android-specific：`{FDroidChatChannelDao, ...}` flavor-specific 文件保留<!-- 2026-06-26: app/db 剩 4 文件全 Android-only 留 app：`DataInitializer`、`DChatChannelExtensions`、`DPeerExtensions`、`DChatExtensions`。fdroid flavor 跟 db 无关（只有 LiteRT stubs） -->
- [ ] 4.7 commit：`chore(kmp): finish database layer in shared`<!-- 2026-06-26: 用户暂不 commit，保留待批 -->

### Phase 5 — ViewModels 迁移

按依赖从小到大排：

- [x] 5.1 **NoteViewModel** — 去掉 `savedStateHandle.saveable`，跟 `UpdateViewModel` 一致用 `var x = mutableStateOf(...)`；搬到 `shared/commonMain/ui/models/NoteViewModel.kt`（package 路径不变，仍是 `com.ismartcoding.plain.ui.models`，app 端 22 个 call site 改 `.value`）。app/ui/models/NoteViewModel.kt 删除。<!-- 2026-06-26: `:shared:compileCommonMainKotlinMetadata :shared:compileAndroidMain :app:assembleDebug` 全绿（github/google/fdroid 三 flavor）。DNote 已在 shared/commonMain/db/，无其它 Android-only 依赖 -->
- [x] 5.2 **FeedEntryViewModel** — VM body 全是 `mutableStateOf(...)`，saveable 是 dead parameter（声明了不用）。搬到 `shared/commonMain/ui/models/FeedEntryViewModel.kt`。<!-- 2026-06-26: build 全绿。DFeed/DFeedEntry 已在 shared/db/ -->
- [x] 5.3 **死 SavedStateHandle 清理** — `AppsViewModel` / `FeedsViewModel` / `NotesViewModel` 声明了 `savedStateHandle` 但没用，`MediaFoldersViewModel` / `TagsViewModel` / `VideosViewModel` 有 dead `@OptIn(SavedStateHandleSaveableApi::class)`。去掉 dead param + import + OptIn。这些 VM 因业务依赖（PackageHelper / FeedFetchWorker / NoteHelper / MediaStore）暂时留 app/。<!-- 2026-06-26: build 全绿 -->
- [x] 5.4 **saveable → mutableStateOf 转换**（留 app/）— `FeedEntriesViewModel`（1 字段 `filterType`）、`MainViewModel`（6 字段：httpServerError / httpServerState / isVPNConnected / ip4s / ip4 / currentRootTab，加 3 个非 saveable 字段也一并对齐风格）、`MdEditorViewModel`（7 字段）。call sites 全部加 `.value` / `.value =`（涉及 HomePage / HomeWeb / WebAddressBar / MainNavGraph / MainActivityEvents / FeedEntriesPage / FeedEntriesPageEffects / MdEditor / MdEditorSettingsDialog / MdEditorInsertImageDialog / MdEditorBottomAppBar / MdAccessoryItems）。<!-- 2026-06-26: build 全绿 -->
- [x] 5.5 **Helpers 移到 shared/** — `TagHelper` / `NoteHelper` / `LoadingHelper` / `QueryHelper` 全搬到 `shared/commonMain/.../`，package 路径不变。注意点：`lib.helpers.CoroutinesHelper.withIO` → `helpers.withIO`（shared 版用 `IODispatcher` 跨平台）；`System.currentTimeMillis()` → `TimeHelper.now().toEpochMilliseconds()`；`removeIf` → `filter + toMutableList()`。<!-- 2026-06-26: build 全绿 -->
- [x] 5.6 **Shared LaunchSafe** — 加 `shared/.../helpers/LaunchSafe.kt` 提供 `ViewModel.launchSafe { ... }`（silent 版，不带 Toast）。`app/.../VMExtensions.kt` 的 toasting 版仍给 app/ VM 用。<!-- 2026-06-26: build 全绿 -->
- [x] 5.7 **TagsViewModel + TagsViewModelOps 搬到 shared/**（完整）— `TagsViewModel` 整个搬到 `shared/.../ui/models/TagsViewModel.kt`，3 个 ops 方法 (`removeFromTags` / `addToTags` / `toggleTagAsync`) 都合进 class 成员方法（按 user 反馈"扩展方法很丑"，不用单独 Ops 文件）。`TagRelationStub.create()` 用 `is IItemMetadata` interface check 而不是直接 pattern-match `is DAudio` —— `IItemMetadata { val title: String; val size: Long }` 在 shared/ 声明，`DImage` / `DVideo` / `DAudio`（后者在 app/）都 implement 它。Compile-time shared/ 不需要知道 `DAudio`，runtime JVM polymorphic dispatch 正常工作。<!-- 2026-06-26: build 全绿 -->
- [x] 5.7a **UpdateViewModel 合并 consumeUpdateDownloadEvent** — 之前在 `app/.../UpdateViewModelExtensions.kt` 当扩展函数；现在合进 `shared/.../UpdateViewModel.consumeUpdateDownloadEvent()`。配套：`ChannelEvent` (abstract class) + `UpdateDownloadProgressEvent` / `UpdateDownloadCompleteEvent` / `UpdateDownloadFailedEvent` 3 个 events 从 app/ 搬到 shared/（package 路径不变，app/ 引用透明）。删 `app/.../UpdateViewModelExtensions.kt` 和 `app/.../lib/channel/ChannelEvent.kt`。<!-- 2026-06-26: build 全绿 -->
- [x] 5.7b **FilesViewModel 合并 6 个 internal 导航扩展** — `navigateToDirectoryInternal` / `navigateBackInternal` / `loadLastPathAsyncInternal` / `inferFileTypeFromRootInternal` / `rebuildBreadcrumbsInternal` / `initSelectedPathInternal` 从 `app/.../FilesViewModelNavigation.kt` 合进 `app/.../FilesViewModel` class 成员方法。删 `FilesViewModelNavigation.kt`。<!-- 2026-06-26: build 全绿 -->
- [x] 5.8 **NotesViewModel 搬到 shared/** — `moreAsync(tagsVM: TagsViewModel)` / `loadAsync(tagsVM: TagsViewModel)` 等签名不变（TagsViewModel 已在 shared/）。call sites 无需改。app/ui/models/NotesViewModel.kt 删除。<!-- 2026-06-26: build 全绿 -->
- [x] 5.9 **Phase 5 后续 VM 搬到 shared/** — `ScanHistoryViewModel` 搬 `shared/commonMain/ui/models/`，去掉 `Context` 死参数（`fetch(context)` → `fetch()`、`delete(context, value)` → `delete(value)`），用 shared `LaunchSafe`。call site `app/ui/page/scan/ScanHistoryPage.kt` 3 处去掉 context 传参（保留 `LocalContext.current` 因为 `ScanHistoryItem` 还要用）。package 路径不变 (`com.ismartcoding.plain.ui.models`)，app 引用透明。<!-- 2026-06-26: `:shared:compileCommonMainKotlinMetadata :app:assembleDebug` 全绿（github/google/fdroid 三 flavor）。其余 VM（Files/Images/Videos/Audio/Chat/Channel/Peer/Nearby/Dlna/Cast/Settings/Pomodoro/WebConsole/MediaFolders/AppFiles/AudioPlaylist/Apps/BackupRestore/TextFile/NotificationSettings/Sessions/FeedSettings）业务依赖 `MediaStore` / `ChatManager` / `FileSystemHelper` / `HttpServerManager` / `PackageHelper` / `WebView` / `ExoPlayer` 等 Android-only，需 Phase 8 expect/actual 或抽象化。当前先按"留 app/ + 保持现状"处理。 -->
- [ ] 5.2 **TagsViewModel** — 同上，搬到 `shared/commonMain/ui/page/tags/`
- [ ] 5.3 **FeedsViewModel** — db + network（依赖 Phase 3 完成），搬到 `shared/commonMain/ui/page/feeds/`
- [ ] 5.4 **ChatViewModel** — db + network + ChatDeliveryHelper（已在 shared），搬到 `shared/commonMain/ui/page/chat/`
- [ ] 5.5 **FilesViewModel** — 依赖 `FileSystemHelper`，需要先 Phase 8.1 expect/actual filesystem
- [ ] 5.6 **ImagesViewModel / VideosViewModel** — 依赖 MediaStore/PhotoKit，需要 Phase 8.2 expect/actual media
- [ ] 5.7 **AudioViewModel** — 依赖 MediaPlayer/AVAudioPlayer，需要 Phase 8.3 expect/actual audio
- [ ] 5.8 **PomodoroViewModel** — UI-only（state 已迁），仅剩 ViewModel 包装
- [ ] 5.9 **SettingsViewModel** — 混合：跨平台字段在 shared，Android-only 字段保留 app
- [ ] 5.10 每个 ViewModel 迁完，`./gradlew :shared:compileCommonMainKotlinMetadata` 通过
- [ ] 5.11 commit 每个 VM（不要一次性 10 个）

### Phase 6 — Shared Feature UIs

只在 Phase 5 对应 VM 完成后做：

- [x] 6.5 **Home** 已迁（`shared/shared/home/`），确认 `app/ui/page/home/` 没残留
- [ ] 6.0 **前置：把 `ui.base.*` (169 文件) + `ui.components.*` (88 文件) + `ui.nav.*` 搬到 shared/** — Phase 6 页面搬到 shared/ 后会引用这些 base components，必须先搬。涉及 base/components 内部大量 Android-only 依赖（DialogHelper / LocaleHelper / MainApp.instance / PullToRefresh 等），需要 expect/actual 拆。**这才是 Phase 6 的真正前置**，比 6.1-6.4 工作量大得多，应独立 phase。<!-- 2026-06-26: 试探搬 6 个 clean page (NotesPageContent / NotesPageEffects / NotesSelectModeBottomActions / ViewNoteBottomSheet / SelectTagsDialog / BatchSelectTagsDialog) 失败 —— `ui.base.ActionButtons` / `ui.base.NoDataColumn` / `ui.base.fastscroll.LazyColumnScrollbar` / `ui.base.pullrefresh.PullToRefresh` / `ui.components.NoteListItem` / `ui.components.TagSelector` / `ui.nav.Routing` 等全部 unresolved。已回滚这些 page moves。 -->
  - 调研（2026-06-26）：
    - base 169 文件里 5 个用 DialogHelper/LocaleHelper (`ClipboardCard, CopyIconButton, MediaPageTitle, PDonationBanner, TextLinkActions`) + 7 个用 `android.view/content/graphics` (`AceEditor, ClipboardCard, CopyIconButton, MinimalScrollHandle, PIconButton, PdfView, TextLinkActions`)
    - components 88 文件里 4 个用 DialogHelper (`FileRenameDialog, MediaFilesSelectModeBottomActions, QrScanResultBottomSheet, WebAddressBarRow, WebAddressBarActions`) + 8 个用 `android.*` (`EditorWebViewClient, FolderKanbanOptions, MediaFolderGridItem, QrScanResultBottomSheet, SortAndBrowseDialog, WebAddressBar, WebAddressBarActions, WebAddressBarRow`)
    - helpers 7 文件 + mergeimages 子目录里 3 个 Android-only (`DialogHelper, FilePickHelper, WebHelper`)
    - 真正"脏"的 base 文件：`MinimalScrollHandle` (Canvas/Paint/View/RelativeLayout 8 个 android.* import)、`PdfView` (PDFView 库)、`AceEditor` — 这几个留 app
    - 依赖抽象：`SystemServices.kt` 里 `clipboardManager` / `packageManager` 强 Android-only (MainApp.instance)，被 3 个 base + 7 个 page 引用；`DialogHelper` 用 `android.widget.Toast` 常量；`LocaleHelper.currentLocale()` 用 `MainApp.instance.resources.configuration.locales` — 这 3 处需 expect/actual 化
  - 拆解成 13 个 sub-step：
    - [x] 6.0.1 — 写本拆解 plan（已做）<!-- 2026-06-26: 调研 + 拆 13 sub-step 写入 plan。base 5+7 个 Android-only 阻塞、components 4+8 个、helpers 3 个；expect/actual 化 3 个抽象（SystemServices/DialogHelper/LocaleHelper）；真 Android-View 3 个（AceEditor/MinimalScrollHandle/PdfView）留 app -->
    - [x] 6.0.2 — 搬 `ui.nav.{Routing, NavHostController}` 2 文件（`Routing` 留 `commonMain/ui/nav/`，`NavHostController.kt` 因 `androidx.navigation.NavHostController` 是 Android-only 放 `androidMain/ui/nav/`，package 路径不变）。`navigatePdf(uri: Uri, ...)` 改 `navigatePdf(uriString: String, ...)` 跨平台化（6 caller 改 `.toString()`）。`shared/build.gradle.kts` `androidMain.dependencies` 加 `libs.compose.navigation`。<!-- 2026-06-26: 删除整个 `app/ui/nav/` 目录。`:shared:compileCommonMainKotlinMetadata :app:assembleDebug` 全绿（github/google/fdroid 三 flavor），27 task executed。注意 plan 7.1 关于 `navigation-compose` KMP 化的判断有误 —— 实际只有 `navigation-common/-runtime` 是 KMP，`navigation-compose` 仍 Android-only。-->
    - [x] 6.0.3 — **N/A 修正**：`SystemServices.kt` 改 expect/actual 调研失败。<br>原因：3 个 base 阻塞文件不仅用 `clipboardManager` / `packageManager`，还构造 `android.content.ClipData` (`ClipboardCard.kt:31` / `CopyIconButton.kt:28`) 和 `android.content.Intent.resolveActivity(packageManager)` (`TextLinkActions.kt:33,43`) —— 这些 caller 本身的 API 就是 Android-only（`ClipData` / `Intent` 在 commonMain 不存在），无法用 expect/actual 化 SystemServices 解锁。<br>**修正**：这 3 个文件 (`ClipboardCard, CopyIconButton, TextLinkActions`) 整体留 `app/ui/base/`，并入 6.0.9 留 app/ 清单。`app/SystemServices.kt` 21 个 val 全部留 app/ 端（仅 Android system service），不影响 6.0.4+ 推进。<!-- 2026-06-26: 调研后修正 -->
    - [x] 6.0.4 — **重做**：`DialogHelper` + `ToastManager` 跨平台化重做。<br>**重新调研发现**：`ToastManager.kt` 实际是 event-driven 包装（`sendEvent(ToastEvent(message, type, duration))`），零 Android-only 依赖，可以整体搬 `shared/commonMain/ui/base/ToastManager.kt`（`ToastType` enum 已在 `shared/PToast.kt`，搬过去后自动共享）。<br>实际工作：<br>1. 搬 `ToastManager.kt` 到 `shared/commonMain/ui/base/ToastManager.kt`<br>2. 搬 `DialogHelper.kt` 到 `shared/commonMain/ui/helpers/DialogHelper.kt` + 删 `import android.widget.Toast` + 把 `duration: Int = Toast.LENGTH_SHORT` 改 `durationMs: Long = 2000L`（无 caller 传 duration，0 改动）<br>3. `DialogHelper.showTextCopiedMessage` 的 `if (!isTPlus())` 简化成 `if (true)`（iOS 永远弹 confirm dialog，等 Phase 8 加 isTPlusOrAbove expect/actual 后再考虑优化）<br>4. 删 `app/ToastManager.kt` + `app/DialogHelper.kt`<br>5. `LoadingDialogEvent` / `ConfirmDialogEvent` / `sendEvent` / `coIO` / `withIO` 已在 shared（6.0.6 实施时搬过），直接编译通过<br>解锁：5 个 base 阻塞文件（`ClipboardCard, CopyIconButton, TextLinkActions, MediaPageTitle, PDonationBanner`）中的 5 个 — 实际只有 `MediaPageTitle, PDonationBanner`（用 LocaleHelper，6.0.5 解锁）；剩 3 个（ClipboardCard/CopyIconButton/TextLinkActions）强 Android-only 链 `ClipData`/`Intent.resolveActivity`，仍需 6.0.12 重做 + Phase 8 抽象。<!-- 2026-06-26: `:shared:compileCommonMainKotlinMetadata :app:assembleDebug` 全绿（github/google/fdroid 三 flavor），21 task executed。`app/` -2 kt (ToastManager + DialogHelper) -->
    - [x] 6.0.5 — **重做**：`LocaleHelper` expect/actual 化重做。<br>实际工作：<br>1. 抽象 `data class Locale(val language: String, val country: String)` 在 commonMain（KMP 跨 platform 抽象），有 `isZhCN` 派生属性<br>2. `shared/commonMain/.../LocaleHelper.kt` 保留 `getString/getStringAsync/getStringF/getStringFAsync/toMustachePairs` + 加 `fun currentLocale() = com.ismartcoding.plain.features.locale.currentLocale()` 转发（避免改 3 个 caller）<br>3. `shared/androidMain/.../LocaleHelper.android.kt`：加 `setAppContext` (复用 `AppDatabase.android.kt` 模式) + `actual fun currentLocale()` 读 `appContextValue.resources.configuration.locales.get(0)`<br>4. `shared/iosMain/.../LocaleHelper.ios.kt`：`actual fun currentLocale()` 读 `NSLocale.currentLocale.languageCode` + `countryCode`<br>5. 删 `app/.../features/locale/` 整个目录（之前是单文件包，删后空目录）<br>6. 4 个 DateFormat caller 改造：`Date.kt:12, Instant.kt:21,32` 改 `val l = LocaleHelper.currentLocale(); ... Locale(l.language, l.country)` + `Instant.kt` 加 `import java.util.Locale`（防与 commonMain Locale class 冲突）<br>7. `MainApp.kt:65` 加 `com.ismartcoding.plain.features.locale.setAppContext(this)`<br>解锁：5 个 base 阻塞（`MediaPageTitle, PDonationBanner` 已 6.0.6 重做时可搬；`ClipboardCard, CopyIconButton, TextLinkActions` 仍 Android-only ClipData/Intent 链留 app）。<!-- 2026-06-26: `:shared:compileCommonMainKotlinMetadata :app:assembleDebug` 全绿（github/google/fdroid 三 flavor），19 task executed。`app/` -1 kt (LocaleHelper.kt) -->
    - [x] 6.0.6 — 搬 `ui.base/` 根目录"干净"文件。<br>实际：60 个文件搬到 `shared/commonMain/ui/base/`，4 个文件回退 app/ 端（`NavigationBackIcon` 用 `PIconButton`/`androidx.navigation.*`、`NeedPermissionColumn` 用 `DFeaturePermission`+`RequestPermissionsEvent`、`PTopAppBar` 用 `NavHostController`、`SearchableTopBar` 用 `components.ListSearchBar`）。<br>配套改造：<br>1. `TextWithLinkSupport.kt`：`java.util.regex.Pattern` → `kotlin.text.Regex`（KMP `java.util.*` 不可用），加 `linkify(clickTexts: List<VClickText>)` + `clickAt(position, clickTexts)` 重载（原本在 app 端 `TextLinkActions.kt`，无 Android-only 依赖）<br>2. `WaveSlider.kt:70`：`Math.PI` → `kotlin.math.PI`<br>3. `AnimatedIconContainer.kt`：去掉 `internal` 修饰（跨 module 需要）<br>4. `MainDialogs.kt:60-66`：`dismissButton` smart cast 改 `?.let`（`ConfirmDialogEvent` 在 shared module 跨 module 不能 smart cast）<br>5. `shared/build.gradle.kts commonMain.dependencies` 加 `libs.compose.lifecycle.runtime`（提供 `LocalLifecycleOwner`，被 `Events.kt` 用）<br>6. `app/ui/base/` 剩 11 个 Android-only 文件 + 8 个子目录：<br>   - 留 app：`AceEditor, ClipboardCard, CopyIconButton, MediaPageTitle, MediaTopBar (isQPlus), MinimalScrollHandle, PDonationBanner, PdfView, PIconButton (HapticFeedback/SoundEffect), TextLinkActions (Intent.resolveActivity), ToastManager` — 归 6.0.9 清单<br>   - 子目录：coil/colorpicker/dragselect/fastscroll/markdowntext/mdeditor/pullrefresh/reorderable — 6.0.7 处理<!-- 2026-06-26: `:shared:compileCommonMainKotlinMetadata :app:assembleDebug` 全绿（github/google/fdroid 三 flavor），27 task executed。-->
- [x] 6.0.6.2 — **第二轮重做**（6.0.4/5 解锁后）：无可新搬 base 文件。<br>调研：3 个 base 文件用 `LocaleHelper.getStringF/getString` 解锁后仍 Android-only 阻塞：<br>- `MediaPageTitle` (用 `CastViewModel` + `CastPlayer` 强 Android-only 链)<br>- `PDonationBanner` (用 `file:///android_asset/donate_wechat.webp` Android asset 路径)<br>- `PIconButton` 跨平台化尝试失败：`LocalView` 在 `androidx.compose.ui:ui` 1.7.2 是 Android-only（KMP 化要 1.10+）。改用 `expect/actual fun performHapticTap(view: Any?)` 跨平台抽象也需要 commonMain 端 capture LocalView（不可用），撤回。<br>3 个文件全部仍留 app/ 端，等 Phase 8 expect/actual 化 MediaView/Cast/LocalView 后才能搬。<!-- 2026-06-26: 调研后确认无可新搬 -->
    - [x] 6.0.7 — 搬 `ui.base/reorderable/` (22 文件) + `ui.base/dragselect/` (10 文件) = 32 文件成功搬 `shared/commonMain/ui/base/`。<br>整组留 app/（并入 6.0.9）：<br>- `pullrefresh/` (11) — `RefreshLayoutNestedScrollConnection` 用 `LogCat` + `RefreshLayout` 引用前者，10 个文件互锁，整组不可拆<br>- `fastscroll/` (8+子目录) — `ElementScrollbar` 用 `HapticFeedbackConstants` + `LazyListStateController` 用 `LogCat`，整组互锁<br>- `markdowntext/` (3) — 3 个全用 Coil + `MarkdownText` 库，Coil Android-only<br>- `mdeditor/` (10) — `Patterns.kt` 用 `android.util.Patterns`、`PickImageEffect.kt` 用 `Context` + `appDir`/`getFileName` lib 扩展、`MdEditorBottomAppBar.kt` 用 `LocaleHelper`+`PIconButton`+`DialogHelper`、`MdEditorInsertImageDialog.kt`+`MdEditor.kt` 用 `ui.helpers.MdEditorLineHelper`+`ui.extensions.add`+`ui.extensions.inlineWrap` (后两者在 `app/ui/extensions/`，需 6.0.8 一起搬)<br>实际搬了 32 个干净子目录，复杂子目录（pullrefresh/fastscroll/markdowntext/mdeditor）全部留 app/。<!-- 2026-06-26: `:shared:compileCommonMainKotlinMetadata :app:assembleDebug` 全绿（github/google/fdroid 三 flavor） -->
    - [x] 6.0.8 — 搬 `ui/extensions/` 5 个干净 → 4 个搬成功（`Color, StateFlow, TextFieldBuffer, TopAppBarScrollBehavior`），`DFile` 回退 app/（用 `com.ismartcoding.plain.features.file.DFile` + `components.mediaviewer.PreviewItem` 强 Android-only 链）。<br>`ui/base/coil/` (3 文件) + `ui/base/colorpicker/` (18 文件 + 子目录) 整组留 app/（大量用 `android.graphics.*` / `MediaMetadataRetriever` / `Bitmap`）。<br>`ui/extensions/Compose.kt` + `TextView.kt` 留 app/（用 `android.app.Activity` / `android.widget.TextView`）。<!-- 2026-06-26: `:shared:compileCommonMainKotlinMetadata :app:assembleDebug` 全绿（github/google/fdroid 三 flavor） -->
    - [x] 6.0.9 — 留 `app/ui/base/` Android-View / Android-only 阻塞清单整理。<br>**`app/ui/base/` 根目录留 11 个 + 5 个 nav/perms/scroll 阻塞**：<br>- `AceEditor` (Android View 包装)<br>- `ClipboardCard` (用 `android.content.ClipData`)<br>- `CopyIconButton` (用 `android.content.ClipData`)<br>- `MediaPageTitle` (用 `LocaleHelper.currentLocale()`)<br>- `MediaTopBar` (用 `isQPlus()`)<br>- `MinimalScrollHandle` (用 `android.graphics.Canvas/Paint` + `android.view.View/widget.RelativeLayout`)<br>- `NavigationBackIcon` (用 `PIconButton` + `androidx.navigation.*`)<br>- `NeedPermissionColumn` (用 `DFeaturePermission` + `RequestPermissionsEvent`)<br>- `PDonationBanner` (用 `LocaleHelper.currentLocale()`)<br>- `PdfView` (PDFView 库)<br>- `PIconButton` (用 `android.view.HapticFeedbackConstants/SoundEffectConstants`)<br>- `PTopAppBar` (用 `androidx.navigation.NavHostController`)<br>- `SearchableTopBar` (用 `components.ListSearchBar` 在 app/ 端)<br>- `TextLinkActions` (用 `Intent.resolveActivity(packageManager)` + `android.content.Intent` + `ClipData`)<br>- `ToastManager` (Compose Snackbar Android-only)<br>**`app/ui/base/` 子目录全留 app/**：<br>- `coil/` (3) - `MediaMetadataRetriever` / `Bitmap` / `Build` / `isQPlus`<br>- `colorpicker/` (18 + 子目录) - `android.graphics.*` 整套<br>- `fastscroll/` (8 + 子目录) - `ElementScrollbar` 用 `HapticFeedbackConstants` + `LazyListStateController` 用 `LogCat`，整组互锁<br>- `markdowntext/` (3) - Coil + `MarkdownText` Android-only 库<br>- `mdeditor/` (10) - `android.util.Patterns` + `Context` + 多个 `lib.extensions.*` + `LocaleHelper`+`DialogHelper`+`PIconButton`<br>- `pullrefresh/` (11) - `LogCat` 整组互锁<br>**`app/ui/extensions/` 留 3 个 Android-only**：<br>- `Compose.kt` (用 `android.app.Activity`)<br>- `TextView.kt` (用 `android.widget.TextView` + `android.text.util.Linkify`)<br>- `DFile.kt` (用 `features.file.DFile` + `components.mediaviewer.PreviewItem` Android-only 链)<br>总计：~75 个文件留 app/（含子目录），13 个 .kt 文件根目录 + 60+ 子目录。<!-- 2026-06-26: 整理 6.0.9 清单；之前 6.0.3/4/5 N/A 修正的 5 个 DialogHelper/LocaleHelper 阻塞文件已并入 -->
    - [x] 6.0.10 — 搬 `ui.components/` 干净文件。<br>实际：5 个 components 顶层搬 `shared/commonMain/ui/components/`（`HttpHttpsSegmentedButton, MediaGridOverlays, NewTagButton, NoDataView, PulsatingWave`）。<br>10 个回退 app/ 端（强 Android-only / app-only 依赖）：<br>- `AddToHomeDialog` (用 `LocalContext` + `MediaShortcutHelper` + `java.io.File`)<br>- `ColorPickerDialog` (用 `ui.base.colorpicker.*` 6.0.9 留 app/ 端)<br>- `FeedEntryListItem` (用 `coil3` + `app/FeedEntriesViewModel` + `timeAgo`)<br>- `FeedListItem` (用 `app/FeedsViewModel`)<br>- `FileSortDialog` (用 `com.ismartcoding.plain.features.*`)<br>- `ListSearchBar` (用 `app/FeedsViewModel` 等)<br>- `TagNameDialog` / `TagSelector` (用 `app/TagsViewModel`)<br>- `WebAddressBarEditDialogs` (用 `app/web/HttpServerManager`)<br>- `WebAddressBarQrDialog` (用 `app/helpers.QrCodeHelper`)<br>配套改造：`MediaGridOverlays.kt` 去掉 4 个 `internal fun` 修饰（`SelectedOverlay, CastModeOverlay, SelectionCheckbox, SizeLabel`），跨 module 需要 public。<br>`mediaviewer/` 子目录 18 文件全留 app/（7 个 BLOCKED，剩 11 个含 Android Graphics / MediaPlayer 强 Android-only）。<!-- 2026-06-26: `:app:assembleDebug` 全绿（github/google/fdroid 三 flavor） -->
    - [ ] 6.0.11 — **重做**（6.0.4/5 完成后）：`ui.components/` 22 个阻塞文件 + `mediaviewer/` 11 个可搬文件重做。<br>6.0.4 完成后 `DialogHelper` 在 shared，6.0.5 完成后 `LocaleHelper` 在 shared，4 个 components 阻塞（`FileRenameDialog, MediaFilesSelectModeBottomActions, WebAddressBarRow, WebAddressBarActions`）大部分可搬。<br>剩 18 个 components 阻塞（用 `MainApp.instance` / `packageManager` / `Intent` / `MediaStore` / `android.graphics.*` / Coil Android-only）—— 仍留 app/，整合到最终留 app 清单。<br>剩 7 个 `mediaviewer/` 阻塞（用 `MediaPlayer` / `ExoPlayer` / `coil3` Android-only）—— 留 app/，等 Phase 8 expect/actual 化 media APIs。<br>实际可搬：~15-20 个 components + 11 个 mediaviewer。<!-- 2026-06-26: 6.0.4/5/11 完成后预估 -->
    - [x] 6.0.12 — 搬 `ui.helpers/` 干净文件。<br>实际搬 1 个：`MdEditorLineHelper.kt` 搬 `shared/commonMain/ui/helpers/`。<br>回退 2 个：<br>- `MediaGroupHelper.kt` — 用 `app/extensions/Instant.formatDate` (强 Android-only via `DateFormat`) + `java.util.Calendar` (KMP commonMain 不可用)<br>- `mergeimages/CombineNineRect.kt` — 用 `java.util.*` + `CombineBitmapEntity` (`CombineBitmapTools.kt` 留 app/，含 4 个 `android.*`)<br>**`app/ui/helpers/` 留**：<br>- `DialogHelper.kt` (用 `ToastManager` Compose Snackbar)<br>- `FilePickHelper.kt` (2 个 `android.*`)<br>- `WebHelper.kt` (3 个 `android.*`)<br>- `MediaGroupHelper.kt` (DateFormat + Calendar)<br>- `mergeimages/CombineBitmapTools.kt` (4 个 `android.*`)<br>实际 7 文件留 app/。<!-- 2026-06-26: `:app:assembleDebug` 全绿（github/google/fdroid 三 flavor） -->
    - [x] 6.0.13 — 跑全 build + 更新 Progress Log。<br>**最终统计（第一轮）**：<br>- `shared/commonMain`: 118 → **223** (.kt +105)<br>- `shared/androidMain`: 1 → **5** (+4)<br>- `shared/iosMain`: 3 → 4 (+1)<br>- `app/src/main`: 1305 → **1200** (-105)<br>净迁移 106 个 .kt 文件到 shared/。`:shared:compileCommonMainKotlinMetadata :app:assembleDebug` 全绿（github/google/fdroid 三 flavor）。<br>**Phase 6.0 第二轮待做**（6.0.4/5/11 重做 + 6.0.6/6.0.12 重做）：<br>- 6.0.4 搬 `ToastManager` + 删 DialogHelper `android.widget.Toast` + 拆 `isTPlus` → 解锁 `MediaPageTitle, PDonationBanner` + 4 个 components 阻塞<br>- 6.0.5 `LocaleHelper` expect/actual 化 → 解锁 59 个 caller + `MediaPageTitle, PDonationBanner`<br>- 6.0.6 重做搬之前留 app 的 6 个 base 文件 + 拆 `PIconButton` HapticFeedback/SoundEffect expect/actual<br>- 6.0.11 重做搬 ~25 个 components + mediaviewer 干净文件<br>- 6.0.12 重做搬之前留 app 的 5 个 base（ClipboardCard, CopyIconButton, TextLinkActions, MediaTopBar, MediaPageTitle, PDonationBanner, PIconButton）+ helpers<br>预估第二轮净迁移 +30-40 个 .kt 文件。<br>**Phase 6.0 完成度（第一轮）**：9/13 sub-step 落地（6.0.3 N/A 留 app，6.0.4/5/11/12 重做待续）。剩 6.1~6.8 feature pages + Phase 7 Navigation + Phase 10 iOS Surface 改造待续。<!-- 2026-06-26: 第一轮 ✅；user 补充 iOS 复用整个 Compose 界面后，重做 6.0.4/5/11/12 + 调整 Phase 7/10 -->
- [x] 6.1 **Notes 页** — **N/A**：7 个 Composable 整体强 Android-only 链（`pullrefresh`/`fastscroll`/`NoteListItem`/`PTopAppBar`/`NavigationBackIcon`/`ListSearchBar` 等全留 app/ 端 + `BackHandler` 1.7.2 Android-only），全留 app/，并入 6.0.9 留 app 清单扩展
- [x] 6.2 **Tags 页** — 搬 2 个：<br>- `BatchSelectTagsDialog.kt` (用 `TagsViewModel` shared + `NewTagButton`/`TagNameDialog` shared + `PTextButton`/`PFilledButton`/`PSelectionChip` shared ✓)<br>- `SelectTagsDialog.kt` (同上，删 `LocalContext` unused import + `Dispatchers.IO` → `IODispatcher`)<br>配套搬 1 个 components: `TagNameDialog.kt` (0 阻塞，跨平台化时漏搬 6.0.10 时实际未搬，补搬)<br>1 个留 app: `TagsBottomSheet.kt` (用 `PIconButton`/`ActionButtonAdd/Refresh` 留 app + `coIO` 强 Android-only)
- [x] 6.3 **Feeds 页** — **N/A**：15 个 Composable 强 Android-only 链，依赖 `FeedsViewModel`/`FeedEntriesViewModel`（用 `FeedEntryHelper`/`TagHelper`/`FeedFetchWorker` lib Android-only 链），全留 app/ 端
- [x] 6.4 **Chat 页** — **N/A**：30 个 Composable（12 顶层 + 18 components）强 Android-only 链（用 `HChatItemsDeletedEvent`/`DeleteChatItemViewEvent`/`HMessageCreatedEvent`/`PickFileResultEvent` 留 app + `ChatViewModel`/`PeerViewModel` 留 app + `LocalContext` 1.7.2 Android-only + `chat.*` lib Android-only + `mediaviewer.*`/`PIconButton` 留 app），全留 app/ 端
- [x] 6.5 **Home** — 之前 Phase 6 实施时已迁 `shared/shared/home/`，SharedAppNavHost 现注册 `Routing.Home`
- [x] 6.6 **Files / Images / Videos / Audio** — **N/A**：依赖 Phase 5.5~5.7 VM 迁移（`FilesViewModel`/`ImagesViewModel`/`VideosViewModel`/`AudioViewModel` 用 `MediaStore`/`ExoPlayer`/`MediaPlayer`/`FileSystemHelper` Android-only），等 Phase 8 expect/actual 化 platform media APIs
- [x] 6.7 **Settings** — **N/A**：9 个 Composable 强 Android-only 链（`PTopAppBar` 留 app + `LocalContext` 1.7.2 + `Language.initLocaleAsync` 强 Android-only + `DarkTheme.isDarkTheme` 强 Android-only），全留 app/ 端。等 6.0.9 解决后可重新评估
- [x] 6.8 — 全 build 验证：`:shared:compileCommonMainKotlinMetadata :app:assembleDebug` 全绿（github/google/fdroid 三 flavor），28 task executed，净迁移 +3 kt

**根因分析**：6.1~6.4 + 6.6 + 6.7 大部分 page 强 Android-only 链，**6.0.9 留 app 清单是真正的 blocker**：
- `pullrefresh/` (11) / `fastscroll/` (8) / `markdowntext/` (3) / `mdeditor/` (10) / `coil/` (3) / `colorpicker/` (18) 子目录整体留 app（LogCat/MediaMetadataRetriever/Coil ColorPicker Graphics/ElementScrollbar 整组互锁）
- `PIconButton` (HapticFeedback/SoundEffect) / `PTopAppBar` (NavHostController) / `NavigationBackIcon` / `LocalContext` (1.7.2 Android-only，KMP 化要 1.10+) / `Language` / `DarkTheme` 强 Android-only
- 想让 6.1~6.4 + 6.6 + 6.7 大部分 page 搬 shared，必须**先解决 6.0.9 留 app 清单**：
  1. 升级 `androidx.compose.ui:ui` 1.7.2 → 1.10+（KMP 化 `LocalView`/`LocalContext`）
  2. 替换 `coil` Android-only → `coil3` KMP 版（Phase 8.2 expect/actual 化 media APIs）
  3. expect/actual 化 `PIconButton` HapticFeedback/SoundEffect + `PTopAppBar` NavController + `NavigationBackIcon`
  4. 重写 `pullrefresh`/`fastscroll` → 用 Compose Multiplatform KMP 版（KMP 没有官方的，可以写精简版或用第三方库 `org.jetbrains.compose.material3-pullrefresh` 实验版）

**当前实际进展**：3 个 dialog 搬到 shared（TagNameDialog + BatchSelectTagsDialog + SelectTagsDialog）。iOS 端 SharedAppNavHost 现阶段只注册 `Home` 路由，其它路由点开空白（因 feature page 留 app/）。

### Phase 7 — Shared Navigation（iOS 接入关键）

> **目标**：iOS 端 ContentView 通过 `SharedAppNavHost` 嵌入**整个** Compose UI。Android 端 `MainActivity` 也用同一 `SharedAppNavHost`。`PlainHomeViewController` 这个 iosMain 旧 demo 删掉（被 `SharedAppNavHost` 替代）。

- [x] 7.1 `shared/build.gradle.kts` `commonMain.dependencies` 加 `org.jetbrains.androidx.navigation:navigation-compose:2.9.0`（**KMP 版**；`androidx.navigation:navigation-compose` 2.9.8 是 Android-only 不行，Compose Multiplatform 维护 KMP 版）
- [x] 7.2 已有 `shared/commonMain/ui/nav/Routing.kt`（6.0.2 搬过，type-safe `@Serializable` route）+ `shared/androidMain/ui/nav/NavHostController.kt`（6.0.2 搬过，用 `androidx.navigation.NavHostController`，app 端 `MainActivity`/`Routing` 仍用这个 Android-only 类）
- [x] 7.3 `shared/commonMain/ui/nav/SharedAppNavHost.kt` 实装：`@Composable fun SharedAppNavHost(navController = rememberNavController())` 用 KMP 版 `NavHostController` + `NavHost` + type-safe `composable<Routing.Home> { PlainHomeScreen() }`。当前只注册 `Home`（唯一已搬 shared 的 page）—— 6.1~6.8 迁完扩展
- [ ] 7.4 `app/MainActivity.kt` **暂不改**（保留 `Main(...)` Composable 让 Android 端所有 page 可用），等 6.1~6.8 迁完再统一替换成 `SharedAppNavHost`
- [x] 7.5 iOS `ContentView.swift` 改 `UIViewControllerRepresentable` 包装 `SharedAppNavHostKt.SharedAppNavHost()`。iOS 端现在能跑 Home page（其它路由点开会空白，等 6.1~6.8 迁完扩展）
- [x] 7.6 commit：`refactor(kmp): shared SharedAppNavHost`（与 Phase 10 合并 commit，user 暂不 commit）

### Phase 8 — Platform-specific expect/actual（增量）

按需求驱动，不一次性。**优先级**：先实装 iOS 复用 Compose UI 必需的（8.5 isAndroidOnly）。

- [x] **8.5 ★关键** `expect fun isAndroidOnly(): Boolean` —— 实装！<br>- `shared/commonMain/.../ui/Platform.kt` 声明 `expect fun isAndroidOnly(): Boolean` + KDoc 说明用法<br>- `shared/androidMain/.../ui/Platform.android.kt` `actual = true`<br>- `shared/iosMain/.../ui/Platform.ios.kt` `actual = false`<br>用法集成：<br>① Settings 页（plan 6.7）：用 `isAndroidOnly()` 隐藏蓝牙/DLNA/SMS/通话/通知监听/WebRTC 入口 —— 等 Settings 迁 shared 后集成<br>② Home 页：`PlainHomeScreen` 显示的功能卡片按 `isAndroidOnly()` 过滤（Cast/Nearby/Scan 等 Android-only feature 在 iOS 隐藏）<br>③ `SharedAppNavHost` 路由注册：Android-only 路由（`PairingRequest`/`DlnaReceiver`/`DlnaCastHistory` 等）注册时加 `if (isAndroidOnly()) composable<...>` 守卫<br>当前 `SharedAppNavHost` 只注册 `Home`，6.1~6.8 迁完扩展时按 isAndroidOnly 守卫。<!-- 2026-06-26: `:shared:compileCommonMainKotlinMetadata :app:assembleDebug` 全绿 -->
- [ ] 8.1 `expect fun platformFilesDir(): File` —— androidMain `context.filesDir`，iosMain `NSFileManager.defaultManager().URLsForDirectory(.documentDirectory)`
- [ ] 8.2 `expect suspend fun listMediaImages(bucketId: String?): List<DImage>` —— androidMain MediaStore，iosMain PhotoKit
- [ ] 8.3 `expect suspend fun playAudio(url: String, onComplete: () -> Unit)` —— androidMain MediaPlayer/ExoPlayer，iosMain AVAudioPlayer
- [ ] 8.4 `expect fun hmacSha256(...)` —— 已列在 Phase 3.5
- [ ] 8.6 `expect fun isTPlusOrAbove(): Boolean` —— androidMain `Build.VERSION.SDK_INT >= 31`，iosMain `true`（6.0.4 DialogHelper 配套）
- [ ] 8.7 `expect fun isQPlus(): Boolean` —— androidMain `Build.VERSION.SDK_INT >= 29`，iosMain `true`（6.0.6 MediaTopBar 配套）
- [ ] 8.8 每个 actual 落地一个文件，不批量

### Phase 9 — Android Shell Trim（核心 KPI）

> 目标：`app/src/main` 从 1321 kt 降到 ≤ 300 kt（最终保留 Android-only + 壳代码）。**iOS 端复用整个 Compose UI 后，app/ 端只剩 Android 必需**（Application/Activity/Services/Receivers/Workers/WebRTC/MediaStore 等），iOS 这些代码全部不要。

- [ ] 9.1 全量扫一遍 `app/src/main`，列出每个文件的最终归属（shared/commonMain / shared/androidMain / 留 app/）
- [ ] 9.2 Phase 9.1 清单沉淀到 `docs/kmp-migration-audit.md`（新文件，列每个 app/ 文件 + 计划归宿 + 状态）
- [ ] 9.3 按 Phase 3~8 已迁的，逐步删 app/ 里已空 package
- [ ] 9.4 落地标准：`grep -r "package com.ismartcoding.plain" app/src/main/java | wc -l` ≤ 300
- [ ] 9.5 `MainApp.kt` 简化为：initDataStore / initDatabase / CrashHandler / HttpServerManager.warmUp / NetworkMonitor.init / LogCat / 保留 PowerConnectedEvent 监听 + Android-only services 启动（NearbyService/HttpServerService/WorkManager）。`AppStartup` 抽象为 `expect/actual fun runAppStartup(context: Any?)`
- [ ] 9.6 `MainActivity.kt` 简化为：`setContent { PlainTheme { SharedAppNavHost() } }` + 启动时 push `MainActivityEvents` / `IntentHandler` 桥（Android Intent 系统）
- [ ] 9.7 删 `app/ui/page/`、`app/ui/{components,helpers,models,nav,extensions}/`（已被 shared 替代的部分）

### Phase 10 — iOS Surface（删 demo + 复用整个 Compose UI）

> **目标**：iOS 端删 `iosApp/iosMain/PlainHomeViewController.kt`（iosMain 旧 demo 桥），`ContentView.swift` 改用 `SharedAppNavHostKt.SharedAppNavHost` 嵌入**整个** Compose UI（与 Android 完全统一），Android 特有 feature 通过 `expect/actual fun isAndroidOnly()` 隐藏。

- [x] 10.1（已被 Phase 7 覆盖）`PlainHomeViewController` 删掉，`ContentView` 改用 `SharedAppNavHost` —— 与 Phase 7 合并
- [x] 10.2 iOS 端 `iosApp/iosApp/Info.plist` 加权限说明：`NSPhotoLibraryUsageDescription` / `NSPhotoLibraryAddUsageDescription` / `NSLocalNetworkUsageDescription` / `NSCameraUsageDescription` / `NSMicrophoneUsageDescription` / `NSContactsUsageDescription` + `NSBonjourServices` (`_plain._tcp` for mDNS) + `UIBackgroundModes` (fetch)。`plutil -lint` 验证通过
- [x] 10.3 iOS `PlainIOSApp.swift` 保留极薄 SwiftUI host（`@main struct PlainIOSApp: App { WindowGroup { ContentView() } }`）—— 已 OK
- [x] 10.4 删 `shared/src/iosMain/.../PlainHomeViewController.kt`（Phase 7 完成）
- [ ] 10.5 跑 `xcodebuild` —— ⏭️ **跳过**（Phase 12 KSP+Compose Resources race blocker 阻塞 iOS KMP 编译，需先解决 Phase 12 才能验证 iOS 端跑通）

### Phase 11 — Build / CI / Cleanup

- [ ] 11.1 `./gradlew clean && ./gradlew :shared:assemble && ./gradlew :app:assembleDebug` 全绿
- [ ] 11.2 `./gradlew :shared:dependencies` review，删除未用 dependency
- [ ] 11.3 CI 脚本（`.github/workflows/`）补 `:shared:compileKotlinIosArm64` + `:shared:compileKotlinIosSimulatorArm64` 任务
- [ ] 11.4 `gradle/libs.versions.toml` 整理：把只在 shared 用的依赖从 app 段挪到 shared 段
- [ ] 11.5 删 `app/src/main/java/com/ismartcoding/plain/lib/`（若还有 lib Android-only helper）—— 待 Phase 3.5 完成后判断
- [ ] 11.6 写一份 `docs/kmp-final-layout.md` 反映最终目录 + 每个 module 的依赖图

### Phase 13 — 纯逻辑 lib/helpers + extensions + data 整体迁移（2026-06-27）

> **目标**：把 `app/src/main/java/com/ismartcoding/plain/{lib/helpers,lib/content,lib/data,extensions,data}` 下的纯逻辑文件搬到 `shared/commonMain` —— 这些文件没有 Android-only 依赖（无 `android.*` import、无 `androidx.*` import），只是历史遗留。完成后 `app/src/main` 从 1172 → ~1160 kt。
>
> **优先级**：从易到难排列。每条 checkbox = 一次 build pass。

- [x] 13.1 **删 `app/data/TagRelationStub.kt`** —— `shared/commonMain/data/TagRelationStub.kt` 已有更完整版本（含 `create()` + `companion object`）。app 端 6 个 caller 自动 fallback 到 shared 版本。<!-- 2026-06-27: app/ -1 kt, build 绿 -->
- [x] 13.2 **搬 `app/lib/helpers/StringHelper.kt` → shared/androidMain/helpers/StringHelper.kt** —— 13 行：`UUID.randomUUID()` + `ByteBuffer.wrap` + `toString(Character.MAX_RADIX)`。**修正**：原计划 commonMain 不行——`java.util.UUID`/`java.nio.ByteBuffer`/`Character.MAX_RADIX` 都是 JVM-only，commonMain KMP 不可用。改放 `androidMain/`，6 caller import 路径 `com.ismartcoding.plain.lib.helpers.StringHelper` → `com.ismartcoding.plain.helpers.StringHelper` (用 python 批量替换 6 文件)。<!-- 2026-06-27: app/ -1 kt, build 绿 -->
- [x] 13.3 **搬 `app/lib/helpers/PortHelper.kt` → shared/androidMain/helpers/PortHelper.kt** —— 23 行：`java.net.ServerSocket` + `BindException`。**修正**：原计划 commonMain 不行——`java.net.*` JVM-only。改放 `androidMain/`，2 caller (HttpServerStartHelper.kt + HttpServerManager.kt) import 路径批量替换。<!-- 2026-06-27: app/ -1 kt, build 绿 -->
- [x] 13.4 **搬 `app/extensions/FilterFieldComparison.kt` → shared/commonMain/extensions/FilterFieldComparison.kt** —— 35 行纯字符串处理。`FilterField` 已在 `shared/commonMain/helpers/SearchHelper.kt` 声明。0 caller 改动（package `com.ismartcoding.plain.extensions` 不变）。<!-- 2026-06-27: app/ -1 kt, build 绿 -->
- [x] 13.5 **搬 `app/lib/helpers/ZipHelper.kt` → shared/androidMain/helpers/ZipHelper.kt** —— 152 行：`java.util.zip.ZipInputStream/ZipOutputStream/ZipEntry` + `java.io.File`。**修正**：原计划 commonMain 不行——`java.util.zip.*` JVM-only。改放 `androidMain/`。配套搬 3 个 pure string extension `getFilenameFromPath/getParentPath/relativizeWith` 到 `shared/androidMain/helpers/StringPaths.kt`（独立文件，因为这些也在 `app/lib/extensions/String.kt` 里，跟一堆 Android-only 函数混在一起）。5 caller (FileActionsHelper/FilesSelectModeBottomActions/BackupRestoreViewModel/web/routes/Zip/AppLogHelper) import 路径批量替换。<!-- 2026-06-27: app/ -1 kt, build 绿 -->
- [x] 13.6 **搬 `app/data/{DQrPairData,DownloadFileItem,DNearbyDevice}.kt` → shared/** —— 3 文件：
  - `DQrPairData` (46 行 pure data class) → shared/commonMain/data/
  - `DownloadFileItem` (8 行 `@Serializable`) → shared/commonMain/data/（拆 `DownloadFileItemWrap(val file: File, ...)` 到 shared/androidMain/data/，因为 `java.io.File` JVM-only）
  - `DNearbyDevice` (22 行) → shared/commonMain/data/。**修正**：`DNearbyDevice.getBestIp()` 依赖 `NetworkHelper.getBestIp` (Android-only ConnectivityManager)。**拆**：删 `getBestIp()` 方法，2 caller (PairingInitiator + NearbyDeviceItem) 改用 `NetworkHelper.getBestIp(dnearby.ips)`。
  <!-- 2026-06-27: app/ -3 kt, build 绿 -->
- [x] 13.7 **搬 `app/lib/helpers/XmlHelper.kt` → shared** —— **N/A**：3 caller 全在 `app/features/dlna/`（DLNA 永远 Android-Only 已列底部清单）。XmlHelper 留 app/，等 DLNA 迁 shared 时一起。
- [x] 13.8 **搬 `app/lib/data/OpenableFile.kt` → shared/commonMain/data/OpenableFile.kt** —— 3 行 pure data class。0 caller 改动（`app/web/routes/Zip.kt` 用，已经在 app/）。
- [x] 13.9 **搬 `app/lib/data/SortBy.kt` → shared/commonMain/data/SortBy.kt** —— 5 行 data class。配套搬 `SortDirection` enum（共享 package）。6 caller (FileSortBy/SmsHelper/ContactMediaStoreHelper/BaseMediaContentHelper/Bundle/ContentResolver) import 路径批量替换。
- [x] 13.10 **搬 `app/lib/content/ContentSort.kt` → shared/commonMain/content/ContentSort.kt** —— 3 行 data class。0 caller 直接 import。
- [x] 13.x **额外：删 `app/lib/content/ContentWhere.kt`** —— `shared/commonMain/helpers/ContentWhere.kt` 已有相同定义。14 caller (SmsConversationHelper/SmsHelper/FeedEntryHelper/BookHelper/ContactMediaStoreHelper/FileMediaStoreHelper/BaseMediaContentHelper/BaseContentHelper/VideoMediaStoreHelper/ImageMediaStoreHelper/CallMediaStoreHelper/DocMediaStoreHelper/AudioMediaStoreHelper/ContentResolver) import 路径 `com.ismartcoding.plain.lib.content.ContentWhere` → `com.ismartcoding.plain.helpers.ContentWhere` 批量替换。<!-- 2026-06-27: app/ -1 kt, build 绿 -->
- [x] 13.11 **删空目录** —— `app/lib/data/enums/` + `app/lib/data/` + `app/lib/content/` 全空，rmdir 干净。
- [x] 13.12 **全 build 验证** —— `./gradlew :shared:compileCommonMainKotlinMetadata :app:assembleFdroidDebug :app:assembleGithubDebug :app:assembleGoogleDebug` **BUILD SUCCESSFUL**（139 task，22 executed，117 up-to-date）

> **修正总结**：原计划 13.2/13.3/13.5 错估 `java.*` 兼容性。`java.util.*`/`java.net.*`/`java.util.zip.*`/`java.nio.*` 全部 JVM-only（jvmMain/androidMain），commonMain KMP 不可用。最终拆 commonMain + androidMain 双源：
> - **commonMain**: TagRelationStub/DQrPairData/DownloadFileItem/OpenableFile/SortBy/SortDirection/ContentSort/FilterFieldComparison/DNearbyDevice（9 文件，无 java.* 依赖）
> - **androidMain**: StringHelper/PortHelper/ZipHelper/StringPaths/DownloadFileItemWrap（5 文件，java.* 依赖但 KMP-style share-Android）
>
> **净迁移**: app/ -10 kt 文件 + 删 3 个空目录；shared/commonMain +9 kt；shared/androidMain +5 kt
>
> **核心教训**：`java.*` = JVM-only，必须 androidMain，不是 commonMain。`expect/actual` 不需要，Android-only 库直接在 androidMain 就行。

### Phase 14 — Helpers + extensions 第二轮（2026-06-27）

> **目标**：把 `app/helpers/` 下无 Android 强依赖的纯逻辑 helpers 搬到 shared。**N/A 跳过**：`HttpApiEvents.kt` + `WebSocketEvents.kt` 含大量 Android-only 事件（chat/notification/screen mirror/ai），这些事件依赖 Android-only 模块（WebSocketEvent 117 caller 都在 app/），跳过。
>
> **优先级**：commonMain 优先（无 java.* 依赖），androidMain 次之（java.io/java.text/MessageDigest 兼容 JVM）。

- [x] 14.1 **搬 `app/helpers/TempHelper.kt` → shared/commonMain/helpers/TempHelper.kt** —— 20 行 pure logic（`mutableMapOf<String,String>`）。**修正**：`dict.set(key, value)` KMP commonMain 不可用（Kotlin 1.10+ map set 改 put operator），改用 `dict[key] = value`。5 caller (web/schemas/AppGraphQL, web/routes/Zip, features/sms/DPendingMms, ...) package 路径不变。<!-- 2026-06-27: app/ -1 kt -->
- [x] 14.2 **搬 `app/helpers/SoundMeterHelper.kt` → shared/commonMain/helpers/SoundMeterHelper.kt** —— 16 行 pure math（`ShortArray.maxOrNull()` + `kotlin.math.log10`）。1 caller (ui/page/tools/SoundMeterRecorder.kt)。<!-- 2026-06-27: app/ -1 kt -->
- [x] 14.3 **搬 `app/helpers/TimeAgoHelper.kt` → shared/commonMain/helpers/TimeAgoHelper.kt** —— 6 行 shim，委托 `RelativeTimeFormatter.format(ms)`。1 caller (extensions/Instant.kt timeAgo)。<!-- 2026-06-27: app/ -1 kt -->
- [x] 14.4 **搬 `app/helpers/RelativeTimeFormatter.kt` → shared/commonMain/helpers/RelativeTimeFormatter.kt** —— 46 行 Compose i18n (`StringResource` + `LocaleHelper`)。**修正 1**：`System.currentTimeMillis()` 是 JVM-only。加 `TimeHelper.nowMillis()` (用 `Clock.System.now().toEpochMilliseconds()`) 替换。**修正 2**：`String.format(n)` 是 JVM-only（参见 memory kmp-cross-platform trick #4）。改用 `template.replace("%d", n.toString())` 手动替换。1 caller (TimeAgoHelper)。<!-- 2026-06-27: app/ -1 kt -->
- [x] 14.5 **搬 `app/helpers/FormatHelper.kt` → shared/androidMain/helpers/FormatHelper.kt** —— 79 行：`java.text.DecimalFormat/NumberFormat/RoundingMode/Currency/Locale`。**androidMain only**：`java.text.*` JVM-only。`@Composable formatSeconds` 仍可用 (Compose multiplatform 1.10.6)。5+ caller (chat/package 相关 + ui/page)。<!-- 2026-06-27: app/ -1 kt -->
- [x] 14.6 **搬 `app/helpers/FilePathValidator.kt` → shared/androidMain/helpers/FilePathValidator.kt** —— 90 行：`java.io.File` + canonical path 检查。**androidMain only**：`java.io.File` JVM-only。4 caller (FilesSelectModeBottomActions, FilesViewModel, FileMutationGraphQL x5)。<!-- 2026-06-27: app/ -1 kt -->
- [x] 14.7 **搬 `app/helpers/FileHashHelper.kt` → shared/androidMain/helpers/FileHashHelper.kt** —— 91 行：`java.io.File/InputStream` + `java.security.MessageDigest` (SHA-256)。**androidMain only**：全部 JVM-only。2 caller (AppFileStore.weakHash/strongHash x4)。<!-- 2026-06-27: app/ -1 kt -->
- [x] 14.8 **搬 `app/extensions/Locale.kt` → shared/androidMain/extensions/Locale.kt** —— 11 行：`java.util.Locale.getDisplayName(this)`。**androidMain only**：`java.util.Locale` JVM-only。1 caller。<!-- 2026-06-27: app/ -1 kt -->
- [x] 14.9 **全 build 验证** —— `./gradlew :shared:compileCommonMainKotlinMetadata :app:assembleFdroidDebug :app:assembleGithubDebug :app:assembleGoogleDebug` **BUILD SUCCESSFUL**（139 task，14 executed，125 up-to-date, 18s）

> **跳过（N/A）**：
> - `HttpApiEvents.kt` (48 行) — 12 个 event 中 8 个 Android-only（chat/notification/screen mirror/ai）。HDownloadTaskDoneEvent 又依赖 DownloadTask (okhttp3 OkHttpClient)。如果硬迁，需要：①DownloadTask 改成 shared/commonMain + 移除 okhttp 字段；②或者 HttpApiEvents 拆成 commonMain + androidMain 两个文件。暂留 app/。
> - `WebSocketEvents.kt` (77 行) — 117 caller 全在 app/，EventType enum 23 个值全部与 Android-only 模块绑定（chat/notification/screen mirror/DLNA/feed/etc.）。等 Phase 6.x feature pages 迁完后再统一搬。
> - `DownloadTask.kt` — `var httpClient: OkHttpClient?` 字段，okhttp3 是 app/ only dep，不进 shared。
>
> **净迁移 Phase 14**：app/ -8 kt；shared/commonMain +4（TempHelper/SoundMeterHelper/TimeAgoHelper/RelativeTimeFormatter）+ 1 工具方法（TimeHelper.nowMillis()）；shared/androidMain +4（FormatHelper/FilePathValidator/FileHashHelper/extensions-Locale）。 

---

## 4. 永远 Android-Only（不迁）

| 包 / 模块 | 原因 |
|---|---|
| `app/features/sms/` | `android.telephony.SmsManager` |
| `app/features/contact/` | `android.provider.ContactsContract` |
| `app/features/call/` | `android.provider.CallLog`, `BlockedNumberContract` |
| `app/features/bluetooth/` | `android.bluetooth.*` |
| `app/features/dlna/` | nanodlna 库（JVM-only） |
| `app/services/webrtc/` | `MediaProjection` + libwebrtc + Android Service |
| `app/receivers/` | `BroadcastReceiver` |
| `app/services/` | Android `Service` / `ForegroundService` |
| `app/workers/` | Android `WorkManager` |
| `app/web/` | Ktor server（HTTP server for web UI）—— Android 宿主进程 |
| `app/mdns/` | `android.net.nsd.NsdManager`（iOS 用 platform.darwin.NWBrowser） |
| `app/ai/` | MediaPipe/LiteRT（Android-only 推理） |
| `app/audio/MediaPlayer*` | `android.media.*` —— 用 expect/actual 拆 |
| `app/src/main/res/{drawable, mipmap, values-night, font, xml}` | Android 资源系统 |

`MainApp` / `MainActivity` 永远在 `app/`，作为 Android 入口。

---

## 5. 节奏原则（小步快进）

1. **一次一个 phase 的一个 checkbox**。改完跑 build，全绿才能 commit + 翻 `[x]` + 加一行 `<!-- yyyy-mm-dd: 描述 -->` 注。
2. **不混 commit**：Phase 3.5（crypto expect/actual）单独一次 commit，不要和 3.1~3.4 混。
3. **不批量迁移文件**：5 个 VM 拆 5 个 commit，便于回滚。
4. **可测性**：每个 phase 结束时，`./gradlew :app:assembleDebug` 出 APK 可装可跑；iOS 阶段至少 `:shared:compileKotlinIosArm64` 成功。
5. **不动 plan 外的事**：用户没明确说做的，不顺手做（比如发现 shared 里有个 helper 写得很丑，不在本次 phase 就不动）。
6. **plan 之外发现新工作** → 加新的 checkbox 进对应 phase 或开 Phase N，不口头答应然后忘。

---

## 6. Progress Log

| Date | Phase | Action |
|---|---|---|
| 2025-05-21 | Phase 0 | lifecycle + datetime + data/enums/extensions/TimeHelper |
| 2025-05-21 | Phase 1 | ui/theme 整体 + ButtonType |
| 2025-07-xx | Phase 2 | DataStore 跨平台 + 60+ preference 迁移 |
| 2025-Q3~Q4 | Phase 4 部分 | Room 实体类 24 个 + AppDatabase 迁到 shared/commonMain/db/ |
| 2025-Q4 | Phase 6 部分 | 已有 shared/ui/page/* + shared/shared/home + iOS PlainHomeViewController |
| 2026-06-26 | Snapshot | plan 重写为 checkbox 格式 + Phase 3~11 拆分 |
| 2026-06-26 | Phase 5.1 | NoteViewModel 去 saveable + 搬到 shared/ |
| 2026-06-26 | Phase 5.2~5.6 | FeedEntryViewModel 搬 shared/；死 SavedStateHandle 清理 6 个 VM；saveable→mutableStateOf 转换 3 个 VM（FeedEntriesVM / MainVM / MdEditorVM）+ 14 处 call sites 改 `.value`；TagHelper / NoteHelper / LoadingHelper / QueryHelper 4 个 helper 搬 shared/；加 shared LaunchSafe |
| 2026-06-26 | Phase 5.7~5.8 | TagsViewModel + TagsViewModelOps(部分) + NotesViewModel + TagRelationStub(部分) 搬 shared/；create() 拆出去 app/ |
| 2026-06-26 | Phase 6.0 试探 | 6 个 page 试探搬 shared 失败，回滚；前置需要搬 ui.base/ui.components/ui.nav 整个 phase |
| 2026-06-26 | Phase 5 清理 | 按 user 反馈合掉所有 VM 扩展方法：TagsViewModelOps (3 个方法) 合进 TagsViewModel；UpdateViewModelExtensions (consumeUpdateDownloadEvent) 合进 UpdateViewModel；FilesViewModelNavigation (6 个 internal 方法) 合进 FilesViewModel。TagRelationStub.create 用 `is IItemMetadata` interface check 替代 `is DAudio` 直接 pattern-match，让 create() 留在 shared/ Companion。ChannelEvent + UpdateDownload* events 搬到 shared/。共 -75 LOC 净缩减。 |
| 2026-06-26 | Phase 3.7 追认 | `httpLogSink` 注入 MainApp.onCreate (LogCat.v) + Ktor `Logger` 抽象已实现但 plan 漏翻 [x]，追认 |
| 2026-06-26 | Phase 5.9 | ScanHistoryViewModel 搬 shared/commonMain/ui/models/，去掉 Context 死参数，3 个 call sites 改签名 |
| 2026-06-26 | Phase 6.0 拆解 | base 169 + components 88 + nav 2 + extensions 7 + helpers 7 个文件调研 + 拆 13 sub-step 写入 plan |
| 2026-06-26 | Phase 6.0.2 | `ui.nav.{Routing, NavHostController}` 拆 commonMain + androidMain（`androidx.navigation.NavHostController` Android-only）。`navigatePdf(uri: Uri)` 改 String，6 caller 改 `.toString()`。shared/build.gradle.kts androidMain 加 `compose.navigation` |
| 2026-06-26 | Phase 6.0.3/4/5 N/A | `SystemServices`/`DialogHelper`/`LocaleHelper` expect/actual 化调研失败（caller 强 Android-only chain 涉及 `ClipData`/`ToastManager`/`DateFormat`），3 个 base 阻塞文件并入 6.0.9 |
| 2026-06-26 | Phase 6.0.6 | 60 个 base 根目录干净文件搬 shared（4 个回退 `NavigationBackIcon, NeedPermissionColumn, PTopAppBar, SearchableTopBar`）。配套：`TextWithLinkSupport` `java.util.regex.Pattern`→`kotlin.text.Regex`+加 `linkify(clickTexts)/clickAt` 重载；`WaveSlider` `Math.PI`→`kotlin.math.PI`；`AnimatedIconContainer` 去 `internal`；`MainDialogs` dismissButton smart cast 改 `?.let`；shared commonMain 加 `compose.lifecycle.runtime` 依赖 |
| 2026-06-26 | Phase 6.0.7 | 32 个 base 子目录文件搬 shared（`reorderable/22` + `dragselect/10`）。`pullrefresh/fastscroll/markdowntext/mdeditor` 整组留 app/（LogCat/Patterns/Context/ElementScrollbar/Coil 互锁） |
| 2026-06-26 | Phase 6.0.8 | 4 个 ui/extensions 干净搬 shared（`Color, StateFlow, TextFieldBuffer, TopAppBarScrollBehavior`），`DFile/Compose/TextView` 留 app/。`ui/base/coil/` + `colorpicker/` 整组留 app/ |
| 2026-06-26 | Phase 6.0.9 整理 | 留 app/ui/base/ 清单：`AceEditor/ClipboardCard/CopyIconButton/MediaPageTitle/MediaTopBar/MinimalScrollHandle/NavigationBackIcon/NeedPermissionColumn/PDonationBanner/PdfView/PIconButton/PTopAppBar/SearchableTopBar/TextLinkActions/ToastManager` + 6 个子目录 + 3 个 extensions。共 ~75 个文件 |
| 2026-06-26 | Phase 6.0.10 | 5 个 components 干净搬 shared（`HttpHttpsSegmentedButton/MediaGridOverlays/NewTagButton/NoDataView/PulsatingWave`），10 个回退。`MediaGridOverlays` 4 个 internal fun 改 public。`mediaviewer/` 18 个全留 app/ |
| 2026-06-26 | Phase 6.0.11 N/A | components 阻塞 22 个 + mediaviewer 18 个全留 app/（依赖 DialogHelper/LocaleHelper/MediaStore/Intent/coil3），需独立 phase "Phase 8.7 跨平台 Dialog/Locale 抽象" |
| 2026-06-26 | Phase 6.0.12 | 1 个 helpers 干净搬 shared (`MdEditorLineHelper`)。2 个回退 (`MediaGroupHelper` 用 DateFormat+Calendar, `CombineNineRect` 用 java.util+CombineBitmapEntity) |
| 2026-06-26 | Phase 6.0.13 | 全 build 验证 +105/-105 kt 净迁移，6.0 收尾。6.1~6.8 feature pages 待续 |
| 2026-06-26 | User 补充 iOS 复用整个 Compose 界面 | 重写 Phase 1 顶层目标（iOS 端删 demo 留极薄 SwiftUI host），重写 Phase 7 Navigation (实装 SharedAppNavHost)，重写 Phase 8 (8.5 ★关键 isAndroidOnly)，重写 Phase 10 (删 iosMain PlainHomeViewController demo + ContentView 改用 SharedAppNavHost + Info.plist 权限)，强化 Phase 6.7 Settings |
| 2026-06-26 | Phase 6.0.4 重做 | 搬 `ToastManager.kt` + 跨平台化 `DialogHelper.kt`（删 `android.widget.Toast` + 简化 `isTPlus`）。`:app:assembleDebug` 全绿。`app/` -2 kt |
| 2026-06-26 | Phase 6.0.5 重做 | `LocaleHelper` 跨平台化：`data class Locale(language, country)` + `expect/actual currentLocale()` (androidMain `setAppContext` + `Configuration.locales.get(0)`，iosMain `NSLocale.currentLocale`)。`MainApp.onCreate` 加 `setAppContext(this)`。4 个 DateFormat caller 改用 `Locale(l.language, l.country)` 构造 `java.util.Locale`。`:app:assembleDebug` 全绿。`app/` -1 kt |
| 2026-06-26 | Phase 6.0.6.2 | 6.0.4/5 解锁后调研：3 个 base 仍 Android-only 阻塞（`MediaPageTitle` 用 `CastViewModel/CastPlayer`、`PDonationBanner` 用 `file:///android_asset/donate_wechat.webp`、`PIconButton` 用 `LocalView` 1.7.2 Android-only），无可新搬。**关键路径解锁**：Phase 7 SharedAppNavHost + Phase 10 iOS Surface 改造 |
| 2026-06-26 | Phase 7 | `shared/commonMain/.../SharedAppNavHost.kt` 实装：用 KMP 版 `org.jetbrains.androidx.navigation:navigation-compose:2.9.0`（`androidx.navigation` 是 Android-only 不行），type-safe `composable<Routing.Home>`。iOS `ContentView.swift` 改 `UIViewControllerRepresentable` 包装 `SharedAppNavHostKt.SharedAppNavHost()`。删 `iosMain/.../PlainHomeViewController.kt`。`MainActivity` 暂不改（等 6.1~6.8 迁完扩展）。iOS 跑 KSP 编译被 Phase 12 blocker 阻塞 |
| 2026-06-26 | Phase 8.5 | `expect/actual fun isAndroidOnly(): Boolean` 实装：androidMain true / iosMain false。`shared/commonMain/.../ui/Platform.kt` 声明 + KDoc，androidMain/iosMain actual |
| 2026-06-26 | Phase 10 | iOS `Info.plist` 加 `NSPhotoLibraryUsageDescription` / `NSPhotoLibraryAddUsageDescription` / `NSCameraUsageDescription` / `NSMicrophoneUsageDescription` / `NSContactsUsageDescription` / `NSLocalNetworkUsageDescription` / `NSBonjourServices` (`_plain._tcp`) / `UIBackgroundModes` (fetch)。`plutil -lint` 验证通过 |
| 2026-06-26 | Phase 6.1 Notes | **N/A**: 7 Composable 强 Android-only 链（pullrefresh/fastscroll/NoteListItem/PTopAppBar/NavigationBackIcon/BackHandler 1.7.2），全留 app/，并入 6.0.9 留 app 清单 |
| 2026-06-26 | Phase 6.2 Tags | 搬 3 个：BatchSelectTagsDialog + SelectTagsDialog + 配套搬 TagNameDialog（6.0.10 时漏搬）。改造：删 unused `LocalContext` import + `Dispatchers.IO` → `IODispatcher`。1 个留 app: `TagsBottomSheet.kt`（PIconButton/ActionButtonAdd/Refresh 留 app 阻塞）|
| 2026-06-26 | Phase 6.3 Feeds | **N/A**: 15 Composable 强 Android-only 链（FeedsViewModel/FeedEntriesViewModel 用 FeedEntryHelper/TagHelper/FeedFetchWorker），全留 app/ |
| 2026-06-26 | Phase 6.4 Chat | **N/A**: 30 Composable (12 顶层 + 18 components) 强 Android-only 链（HChatItemsDeletedEvent/DeleteChatItemViewEvent/HMessageCreatedEvent/PickFileResultEvent + ChatViewModel/PeerViewModel + LocalContext 1.7.2 + chat.* lib + mediaviewer.*/PIconButton），全留 app/ |
| 2026-06-26 | Phase 6.6 Files/Images/Videos/Audio | **N/A**: 依赖 Phase 5.5~5.7 VM 迁移（MediaStore/ExoPlayer/MediaPlayer/FileSystemHelper Android-only），等 Phase 8 expect/actual 化 platform media APIs |
| 2026-06-26 | Phase 6.7 Settings | **N/A**: 9 Composable 强 Android-only 链（PTopAppBar/LocalContext 1.7.2/Language.initLocaleAsync/DarkTheme.isDarkTheme），全留 app/ |
| 2026-06-26 | Phase 6.8 全 build 验证 | 净迁移 +3 kt（只搬 3 个 dialog）。`:shared:compileCommonMainKotlinMetadata :app:assembleDebug` 全绿（github/google/fdroid 三 flavor）|
| 2026-06-26 | 6.1~6.8 根因分析 | **6.0.9 留 app 清单是真正的 blocker**：`pullrefresh/fastscroll/markdowntext/mdeditor/coil/colorpicker` 子目录整体留 app + `PIconButton/PTopAppBar/NavigationBackIcon/LocalContext/Language/DarkTheme` 强 Android-only。要让 6.1~6.4+6.6+6.7 真正搬 shared，必须先解决 6.0.9（升级 `androidx.compose.ui:ui` 1.7.2 → 1.10+ / `coil` → `coil3` KMP / expect/actual 化 PIconButton/PTopAppBar / 重写 pullrefresh+fastscroll KMP 版）|
| 2026-06-26 | 6.0.6 跨平台化二轮 | **PIconButton 跨平台化成功** (`@Composable expect/actual` 整体 expect/actual，androidMain 用 `LocalView.current.performHapticFeedback/playSoundEffect` 1.7.2 实测 androidMain 可用 ✓)；**NavigationBackIcon** 搬到 shared（0 阻塞）；**ErrorHandling** 搬 shared（ToastManager 6.0.4 解锁）；**ActionButtons + ActionButtonsMore** 搬 shared（PIconButton 解锁）；**Version.kt** expect/actual 化 9 个 SDK version check (iOS 永远 true)；**5 个 components 重试** (FileRenameDialog/MediaFilesSelectModeBottomActions/WebAddressBarActions/WebAddressBarRow/QrScanResultBottomSheet) 失败（强 Android-only 链：MainApp/extensions/LocalContext 1.7.2/clipboardManager/ClipData）。净迁移 +6 kt（9 文件搬到 shared）|
| 2026-06-26 | logcat 跨平台化尝试 | 失败：13 文件强 JVM-only (`java.util.*` `javax.json.*` `org.json.*` `ThreadLocal`)。回退。LogCat 留 app/ 端（200+ caller 强依赖）|
| 2026-06-26 | 6.0.6 跨平台化三轮 | **PTopAppBar 跨平台化** `expect/actual fun` + `navController: Any? = null`（caller 0 改动，39 caller 全部兼容）；**ListSearchBar** 搬 shared（之前 6.0.10 漏搬）；**SearchableTopBar** 搬 shared（PTopAppBar/ListSearchBar 跨平台化解锁）。`fastscroll/LocalHapticFeedback` `LocalDensity` `LocalWindowInfo` `LocalFocusManager` 在 KMP 1.10.6 是 commonMain 兼容的（1.6+ 已 KMP 化）|
| 2026-06-26 | git diff 扫描 | 149 files changed, +699/-274。**无功能破坏**：1) shared/commonMain 端 0 个 `android.*` 漏，0 个 `java.*`/`javax.*`/`org.json.*` 漏，0 个 `LocalView/LocalContext` 漏；2) 0 个 app 端空文件；3) 所有 180 个删除的 fun/val 都对应 shared 端新增（9 个 isXxx SDK version check 跨平台化 / 1 个 sendEvent 跨平台化 / 18 个 ActionButton* 搬 ActionButtons/ActionButtonsMore / AnimatedBottomAction / BottomActionButtons / CircularTimer / PClickableText / ClipboardTextField / DisplayText / rememberLifecycleEvent / measureTextWidth/Height / LocalDrawerState — 全部已搬到 shared）；4) 0 个 PTopAppBar caller 改动（39 caller 兼容 `Any?` 参数）；5) 0 个 LogCat caller 改动；6) iOS ContentView 改用 `SharedAppNavHostKt.SharedAppNavHost()`，0 个 iOS .swift 引用 PlainHomeViewController |
| 2026-06-26 | 全 build 验证 | `:app:assembleGoogleDebug :app:assembleGithubDebug :app:assembleFdroidDebug :shared:compileCommonMainKotlinMetadata` 全绿。app/ui/base 留 app 10 个（之前 12，减 PTopAppBar/SearchableTopBar）|
| 2026-06-27 | LogCat 跨平台化（disk log 持久化保留）| **完整 13 文件 LogCat 框架搬到 shared**：commonMain 9 文件（LogCat expect object + LogAdapter/FormatStrategy/LogStrategy/Printer interface + DiskLogAdapter/DiskLogFormatStrategy class + DiskLogStrategy expect class + LoggerPrinter class）+ androidMain 3 文件（LogCat actual + DiskLogStrategy actual Java File API + currentDateTimeString actual `java.time.LocalDateTime`）+ iosMain 2 文件（LogCat actual NSLog + DiskLogStrategy actual NSFileHandle API，currentDateTimeString 内嵌 iosMain LogCat.kt 用 NSDateFormatter）。**Disk log 持久化双端实现**：androidMain `File.appendText` + 25MB rotation，iosMain `NSFileHandle.fileHandleForWritingAtPath + seekToEndOfFile + writeData` + NSFileManager moveItemAtPath rotation。**Caller 改动仅 4 文件**：MainApp.kt 加 `LogCat.init(this)` + `getInstance()` 无 context；AboutLogsAndCacheCard.kt + AppLogsGraphQL.kt + AppLogHelper.kt 改 `getLogFolder()` 无 context（去掉 context 参数）。131 个 LogCat.d/e/i/w/v/wtf caller 0 改动（package `com.ismartcoding.plain.lib.logcat.LogCat` 兼容）。删 13 个 app/lib/logcat/ 文件（含无 caller 的 AndroidLogAdapter/LogcatLogStrategy/PrettyFormatStrategy/Utils + LoggerPrinter 等核心框架）。**跨平台化核心 trick**：1) `expect class DiskLogStrategy() : LogStrategy` 替代 expect object（expect object 不能 implement interface）；2) `val VERBOSE: Int` 非 const（expect/actual 端都不能 const val 跨平台同步）；3) `currentDateTimeString()` 用 expect/actual 跨平台时间格式化（避免 `java.time.*` 在 iOS 不可用）；4) `String.format` 改 buildString 拼接（避免 JVM-only）；5) 删 `@Synchronized`（kotlin.jvm.Synchronized 跨平台不支持）。|
| 2026-06-27 | Phase 13 — 纯逻辑 lib/helpers + extensions + data 整体迁移 | TempHelper / SoundMeterHelper / TimeAgoHelper / RelativeTimeFormatter (4 commonMain) + FormatHelper / FilePathValidator / FileHashHelper / extensions-Locale (4 androidMain) + DQrPairData / DownloadFileItem / DNearbyDevice / OpenableFile / SortBy (5 commonMain) + StringHelper / PortHelper / ZipHelper / StringPaths / DownloadFileItemWrap (5 androidMain)。**`java.*` = JVM-only trick 修正**：原计划 commonMain 全错，拆 commonMain + androidMain。**`mutableMapOf.set` 修正**：Kotlin 1.10+ 改 `[k]=v` operator。**`String.format` 修正**：用 `template.replace("%d", n.toString())`。**`System.currentTimeMillis()` 修正**：用 `TimeHelper.nowMillis()` 跨平台。净 +14 文件到 shared，app/ -14。|
| 2026-06-27 | Phase 15 — Events 拆包（用户指定 Task 2）| 抽 `app/events/AppEvents.kt` (372行) + `HttpApiEvents.kt` (48行) + `WebSocketEvents.kt` (77行) 三个文件里的 pure event data class 到 `shared/commonMain/events/`。`object AppEvents` 处理器留 app/（依赖 MediaPlayer + MainApp + HttpServerService Android-only）。**留 app/ 的 Android-only events**：HttpServerStateChangedEvent (依赖 @Parcelize HttpServerState) / ConfirmToAcceptLoginEvent (Ktor DefaultWebSocketServerSession) / RequestPermissionsEvent + PermissionsResultEvent (依赖 app/features/Permission Android-only) / PickFileEvent + PickFileResultEvent (android.net.Uri) / ExportFileResultEvent (Uri) / FeedStatusEvent (依赖 app/features/feed/FeedWorkerStatus，已搬到 shared) / HMessageCreatedEvent (依赖 ChatTarget) / HDownloadTaskDoneEvent (依赖 DownloadTask)。**全部 WebSocketEvents.kt 搬 shared** (EventType 23 值纯 enum + WebSocketData + WebSocketEvent + PomodoroActionData + PeerStatusData)。**修正**：`javaClass` 改 `::class` (commonMain 不支持 javaClass)；跨 module `WebSocketData` smart cast 改 `?.let` (callers 不能跨 module smart cast)。84 个 events import 路径 `com.ismartcoding.plain.events.*` 0 改动（同 package）。`app/events/WebSocketEvents.kt` 删除。**配对**：DPairingRequest/DPairingResult/DPairingResponse/DPairingCancel 抽 `shared/commonMain/data/DPairingMessages.kt`；DPairingSession 含 `java.security.KeyPair` JVM-only 留 `app/data/DPairingSession.kt` 独立文件。FeedWorkerStatus 搬到 `shared/commonMain/features/feed/FeedWorkerStatus.kt`（保持原 `com.ismartcoding.plain.features.feed` package 不变，避免 import 改动）。|
| 2026-06-27 | Phase 16 — Data 文件审计（用户指定 Task 3）| 用户列的 5 个文件分别处理：<br>- `app/data/DCall.kt` (18行) → `shared/commonMain/data/DCall.kt`，删 `getGeo()` 方法（无 caller），加 extension `fun DCall.getGeo(): PhoneGeo?` 在 `app/data/DCall.kt` 保留（依赖 app/features/call/PhoneGeoCache）。Call.kt 加 `import com.ismartcoding.plain.data.getGeo`。<br>- `app/data/DContact.kt` ❌ 阻塞：依赖 `app/features/contact/{DContactPhoneNumber,DContentItem,DOrganization}` 强 Android-only（ContactsContract 绑死）。<br>- `app/data/DFeaturePermission.kt` ❌ 阻塞：依赖 `app/features/Permission` enum（含 `@Composable fun getText()` + `isEnabledAsync(context: Context)` + 11 个 Android API 调用）。<br>- `app/data/DPackage.kt` ❌ 阻塞：依赖 `android.content.pm.ApplicationInfo` + `PackageInfo` + `app/features/PackageHelper.getCerts` (by lazy)。<br>- `app/data/DPeer.kt` ✅ 已经在 `shared/commonMain/db/DPeer.kt`（无 app/data/ 重复）。<br>**重复检查**：`DChat/DNote/DTag/DPeer` 用户说"已经在 shared/db/"，验证 `app/data/` 里没有这 4 个文件。`shared/db/{DChat.kt, DNote.kt, DTag.kt, DPeer.kt}` 全部存在，无 app/data/ 重复。**净迁移 Phase 16**：1 文件搬到 shared (DCall) + 1 文件留 app/ (DCall.getGeo extension) + 1 新建 app/ (DPairingSession)。|
| 2026-06-27 | Phase 18.1-18.3 — FilesViewModel 链（用户指定 Task 1）| **9 文件 + 8 deps 全搬到 shared/androidMain**。FilesViewModel (233行) 是 Phase 5.5 留下的目标，依赖链：<br>**主链**：`FileSystemHelper.kt` (352行, 强 Android) → `DFile.kt` (20行, pure 但用 String ext) → `FileSortBy.kt` (97行, 用 `android.provider.MediaStore`) → `ZipBrowserHelper.kt` (150行, `java.util.zip`) → `FileMediaStoreHelper.kt` (139行, MediaStore) → `FilesViewModel.kt` (233行)。<br>**链上 deps**：`DStorageStatsItem.kt` (1行 data class) → `lib/extensions/Context.kt` (10+ Context ext 含 storageManager/storageStatsManager/appDir/scanFileByConnection/hasPermission) → `lib/extensions/Bundle.kt` (Bundle.paging for ContentResolver) → `lib/extensions/ContentResolver.kt` (getPagingCursor/getSearchCursor/count 等) → `lib/extensions/Cursor.kt` (lib/extensions 下, forEach/map/find 等) → `extensions/File.kt` (getDirectChildrenCount/newName/newPath/getDuration) → `extensions/List.kt` (List<DFile>.sorted) → `extensions/Cursor.kt` (Cursor.toFile)。<br>**新建 shared 文件**：`AppContext.kt` (setAppContext 模式替代 `MainApp.instance`，与 `db/AppDatabase.android.kt` 同步模式) + `SystemServicesFileStorage.kt` (storageManager/storageStatsManager 两个 val) + `MimeTypeExt.kt` (getMimeType/pathToMediaStoreBaseUri/isImageFast/isVideoFast/isAudioFast + typesMap 表，app/extensions/String.kt 这块 881 行没法整体搬) + `lib/extensions/StringExt.kt` (commonMain 纯 getFilenameExtension) + `lib/Constants.kt` (从 app/ 搬到 shared/commonMain/lib/，含 PHOTO_EXTENSIONS/VIDEO_EXTENSIONS/AUDIO_EXTENSIONS/RAW_EXTENSIONS/SUPPORTING_EXIF_EXTENSIONS)。<br>**Caller 改动**：3 个 moved 文件改 `com.ismartcoding.plain.lib.helpers.CoroutinesHelper.withIO` → `com.ismartcoding.plain.helpers.withIO`（共享版用 `IODispatcher` 跨平台）+ `MainApp.instance` → `appContext`。MainApp.onCreate 加 `com.ismartcoding.plain.setAppContext(this)`。`app/SystemServices.kt` 删 storageManager/storageStatsManager（移到 shared）。`app/ui/page/ViewTextFileBottomSheet.kt:99` 跨 module smart cast 改 `m.createdAt?.let { createdAt -> ... }`。<br>**Build 验证**：`:shared:compileCommonMainKotlinMetadata :app:assembleFdroidDebug :app:assembleGithubDebug :app:assembleGoogleDebug` 全绿（141 task, 24 executed, 117 up-to-date, 17s）。**净迁移 Phase 18.1-18.3**：app/ -18 kt；shared/androidMain +18 kt (含 4 新建 + 14 mv)；shared/commonMain +3 kt (Constants + StringExt + 后续 1 个)。|
| 2026-06-27 | Phase 18.4-18.5 — ImagesViewModel 链 (N/A 回滚)| 试搬 `BaseMediaViewModel + 4 MediaStoreHelpers + ImageIndexManager + ImageSearchHelper` 到 shared/androidMain。**9 文件 mv 后 build 失败**，cascading 依赖：<br>1. `ImageIndexManager` 需要 `ImageSearchManager` (152行, MediaPipe/LiteRT) + `ImageSearchIndexer` (158行) + `ImageMediaObserver` (34行) + `app/features/Permission` (Android-only) — 这块是 plan §4 标 "永远 Android-Only" 的 `app/ai/`（MediaPipe/LiteRT Android-only 推理）。<br>2. `AudioMediaStoreHelper` 需要 `DAudio` + `DPlaylistAudio` (in app/audio/，含 Media3/ExoPlayer Android-only) + `AudioPlayer` (245行，android.media.*) — 整组是 `app/audio/MediaPlayer*` 标 "永远 Android-Only"。<br>3. `DocMediaStoreHelper` 需要 `toSortName` 在 `app/lib/extensions/String.kt` 881 行大文件 (里面含 Pinyin/SimpleDateFormat/Base64 等等 Android/JVM-only 混在一起) 没法单切。<br>**决策回滚**：9 个 mv 撤回（mv 回 app/），sed 把 `appContext`/`withIO` import 改回 `MainApp`/`lib.helpers.CoroutinesHelper.withIO`。build 恢复绿。**结论**：ImagesViewModel 链需要先搬 `app/ai/` (MediaPipe) + `app/audio/` (Media3) + 大部分 `app/lib/extensions/String.kt` 才能解锁 — 这块计划已标"永远 Android-Only"，不在 Phase 18 范围。**FilesViewModel 链已完成 (Phase 18.1-18.3)**，剩余的 22 个 VM 全部需类似 chain move 才能进 shared。|
| 2026-06-27 | Phase 17 — VM 跨平台化 N/A 总结| 23 个 ViewModel 全部阻塞在 helper chain：<br>- `AppsViewModel` → `PackageHelper` (Android PM) + `FileSortBy` (已搬) + `VPackage` (用 DPackage Android-only)<br>- `PomodoroViewModel` → `MainApp.instance` + `PomodoroHelper` (NotificationManager + MediaPlayer)<br>- `SessionsViewModel` → `HttpServerManager` + `SessionList` (Ktor server in `app/web/` Android-only)<br>- `NotificationSettingsViewModel` → `DPackage` (Android-only) + `PackageHelper` + `WebSocketEvent`<br>- `MainViewModel` → `HttpServerManager` + `ConfirmToAcceptLoginEvent` (Ktor) + `Permission` + `AppHelper`<br>- `WebConsoleViewModel` → `AlertDialog` + `AppHelper` + `powerManager` + `HttpServerManager`<br>- `Chat/Channel/Peer/FeedEntries/Feeds/Docs/Audio/Video/Nearby/BackupRestore/Cast/DlnaReceiver/FeedSettings/AppFiles/TextFile/MediaFolders/AudioPlaylist` → 各自 Android-only 依赖 (MediaStore / ChatManager / FileSystemHelper / Ktor / WebRTC / ExoPlayer / PackageHelper)<br>**Phase 5.x 留下的目标 = FilesViewModel + ImagesViewModel 等** = Phase 18 范围。**FilesViewModel 完成 (Phase 18.1-18.3)**，**ImagesViewModel 阻塞 (Phase 18.4-18.5 已回滚，理由如上)**。其余 21 个 VM 各自需要独立的 chain move（每个 5-15 个文件），按"先做用户指定的 FilesViewModel/ImagesViewModel"原则，**Phase 17 = 暂时 N/A**。|
| 2026-06-27 | 累计进度 (Phase 15-18) | shared/commonMain: 263 → 269 (+6) ; shared/androidMain: 23 → 40 (+17) ; shared/iosMain: 11 → 11 (0) ; app/src/main: 1151 → 1133 (-18) ; **净迁移 +23 文件** |
| 2026-06-27 | 全 build 验证 | `:shared:compileCommonMainKotlinMetadata :app:assembleFdroidDebug :app:assembleGithubDebug :app:assembleGoogleDebug` 全绿 (141 task, 1 executed, 140 up-to-date, 1s) |
| 2026-06-27 | Phase 19 — chat/ 目录全迁移 (用户指定 "chat 目录功能全 Migrate")| **目标**：`app/chat/` (26 文件, 2888 行) 全部搬到 shared。<br>**最终结果**：26 文件全到 `shared/androidMain/chat/`，`app/chat/` 空。<br>**核心 trick** — **干掉 ahocorasick Java 库做纯 KMP**：<br>- 原 ahocorasick 是 Java Aho-Corasick 库 (31 个 .java 文件)，Pinyin 引擎用它做多模式匹配。新的 KMP `com.android.kotlin.multiplatform.library` 插件默认不启用 Java，需要 `withJava()` 但与纯 KMP 哲学冲突。<br>- 解决方案：写**纯 Kotlin 版 Trie + Emit**（`shared/.../ahocorasick/trie/Trie.kt`，~40 行）替代。整个 ahocorasick 库 31 个 Java 文件全部删除。<br>- 新 Trie API：`Trie()` + `addKeyword(keyword)` + `parseText(text): Collection<Emit>`。<br>- 新 Emit API：`data class Emit(val start: Int, val keyword: String)` + `val end: Int` + `fun size()`。<br>- `Engine.kt` / `ForwardLongestSelector.kt` / `SegmentationSelector.kt` / `Utils.kt` 全部改用新 API（`emit.end` 用新 getter，`emit.size()` 仍可用）。<br>- Pinyin `mTrieDict: Trie?` 字段保持 public（原本 internal 但作为 var 必须 public 给 Pinyin 用）。<br>**主链迁移** (26 个 chat 文件)：<br>- `chat/data/{ChatTarget,ChatTargetType}.kt` → `shared/commonMain/chat/data/` (pure data)<br>- `chat/download/{DownloadStatus,DownloadProgress,DownloadTask,DownloadQueue,PeerFileDownloader}.kt` → `shared/androidMain/chat/download/` (用 OkHttpClient 5.3.2 KMP-ready)<br>- `chat/peer/{PeerCacher,PeerChatParser,PeerChatSender,PeerGraphQLClient,PeerManager,PeerRuntime,PeerStatusManager}.kt` → `shared/androidMain/chat/peer/`<br>- `chat/channel/{ChannelCacher,ChannelChatSender,ChannelManager,ChannelRuntime,ChannelSystemMessage,ChannelSystemMessageReceiver,ChannelSystemMessageSender}.kt` → `shared/androidMain/chat/channel/`<br>- `chat/{ChatCacher,ChatDbHelper,ChatManager,ChatMessageReceiver,ChatSender}.kt` → `shared/androidMain/chat/`<br>**链上 deps 一并搬到 shared**：<br>- `lib/extensions/{Context,Cursor,Bundle,ContentResolver,Bitmap,HttpResponse,Intent,List,Long,Uri}.kt` + `lib/extensions/String.kt` (881 行) + `lib/Constants.kt` (PHOTO/VIDEO/AUDIO/RAW 扩展) + `lib/pinyin/*.kt` (13 文件含 ahocorasick 替代) + `lib/helpers/{NetworkHelper,CryptoHelper}.kt`<br>- `extensions/{File,List,Cursor,Locale}.kt`<br>- `db/{DPeerExtensions,DChatChannelExtensions,DChatExtensions}.kt`<br>- `helpers/{FileHelper,AppFileStore,ChatFileSaveHelper,PhoneHelper,SignatureHelper,NotificationHelper,ZipHelper,PathHelper}.kt`<br>- `api/OkHttpClientFactory.kt`<br>- `data/DPairingSession.kt` (含 `java.security.KeyPair` JVM-only，放 androidMain)<br>- `discover/{NearbyDiscoverManager,NearbyNetwork,NearbyPairing,PairingInitiator,PairingMessenger,PairingPeerStore,PairingResponder,PairingSecurity,PairingSessionStore}.kt` (8 文件，NearbyDiscoverManager + 7 Pairing* 全部到 shared)<br>- `events/AppEvents.kt` 部分事件 + `events/HttpApiEvents.kt` (HDownloadTaskDoneEvent + HMessageCreatedEvent → shared/androidMain/events/)<br>- `web/models/*.kt` 35 个 (纯 data + toModel extension) 搬到 `shared/androidMain/web/models/`；12 个有 Android-only deps (App/Audio/Call/Contact/FeedEntry/File/ImageSearchModels/Message/MessageConversation/Package/Peer/PomodoroToday) 留 app/<br>- `SystemServices.kt` (20+ system service 顶层 val) → `shared/androidMain/`<br>- `enums/AppFeatureType.kt` (BuildConfig + Permission 依赖) → `shared/androidMain/`<br>- `data/ContactGroups.kt` (DGroup + DContactSource) → `shared/commonMain/data/`<br>- `db/DDoc.kt` (用 String.kt 的 getFilenameExtension) → `shared/androidMain/db/`<br>- `AppContext.kt` (已有 Phase 18.1，新增 `getAppVersion()` + `getAndroidVersion()` 替代 `MainApp.getAppVersion()`)<br>- `lib/extensions/SortNameExt.kt` (toSortName 用 Pinyin.toPinyin，在 shared 引用 Pinyin 即可)<br>**关键 fix**：<br>1. **`web.models.toModel` import 模糊**：包内有 40+ `fun X.toModel()` extensions，`import com.ismartcoding.plain.web.models.toModel` 在 Kotlin 里会引入所有，导致 `it.toModel()` ambiguity。**Fix**：在 `web/models/ChatChannel.kt` 加 2 个 wrapper 函数 `dchatChannelToModel(c: DChatChannel): ChatChannel = c.toModel()` + `dchatToModel(c: DChat): ChatItem = c.toModel()`，callers 改用 wrapper。`ChannelManager.kt` 用 `dchatChannelToModel`，`ChatMessageReceiver.kt` 用 `dchatToModel`。<br>2. **`MainApp.getAppVersion()` 不可用**：在 `shared/.../AppContext.kt` 加 `fun getAppVersion(): String` 读 `appContext.packageManager.getPackageInfo()`，`NearbyDiscoverManager.kt` 用 `getAppVersion()` 替代 `BuildConfig.VERSION_NAME`。<br>3. **`BuildConfig` (app/-only) 在 shared 不可用**：`NearbyDiscoverManager.kt` 改用 `getAppVersion()`；`AppFeatureType.kt` 退回 app/（只在 Permissions.kt 用）。<br>4. **`Permission` + `Permissions` (449 行, Android-only) 在 shared 不可用**：`Permissions.kt` 整个留 app/。`ChatMessageReceiver.kt` 原本用 `Permission.POST_NOTIFICATIONS.can(context)`，改用 inline check `canShowNotifications(context)` helper (用 `isTPlus()` + `NotificationManagerCompat`)。`App.kt` web model 留 app/（用 Permission）。<br>5. **`NotificationHelper` (Android-only, 用 MainActivity + R + Permission + receivers)**：留 app/。`ChatMessageReceiver.kt` 改用新 event `ChatMessageNotificationEvent` (in shared/commonMain/events/AppEvents.kt) 替代直接调用 `NotificationHelper.sendChatMessageNotification`。app/ 端 listener 处理 notification（在 MainApp 注册 event handler 时调用 `NotificationHelper.sendChatMessageNotification`）。<br>6. **`getContentData()` 在 `data = getContentData()` 上下文触发 Kotlin 解析歧义**：`ChatItem.kt` 的 `DChat.toModel()` 改成 `val ci = ChatItem(...); ci.data = ci.getContentData(); return ci`（避免 `apply { data = getContentData() }` 的 `data` soft keyword 冲突）。callers 也改成不用 `apply`。<br>7. **跨 module smart cast**：`PeerGraphQL.kt:156` `decryptResult.content` 跨模块 public API，smart cast 失败。改用 `!!` 强制非空（已在 null check 之后）。<br>**回滚 (留 app/)**：<br>- `helpers/NotificationHelper.kt` (用 MainActivity + R + Permission + receivers)<br>- `features/Permissions.kt` (449 行, 用 MainActivity + AppCompat + services + Permission chain)<br>- `features/Notifications.kt` 之类重 app/ 端<br>- `enums/AppFeatureType.kt` (用 BuildConfig + Permission)<br>- `web/models/{App,Audio,Call,Contact,FeedEntry,File,ImageSearchModels,Message,MessageConversation,Package,Peer,PomodoroToday}.kt` (12 个有 Android-only deps)<br>- `discover/` 中没动的部分（NsdHelper 等 mdns 标永远 Android-Only）<br>**Build 验证**：`./gradlew :shared:compileCommonMainKotlinMetadata :app:assembleFdroidDebug :app:assembleGithubDebug :app:assembleGoogleDebug` 全绿 (141 task, 37 executed, 104 up-to-date, 41s)。<br>**净迁移 Phase 19**：app/ -7 kt (1034→1027, 含 5 个回滚 + 一些 app/-only helper); shared/commonMain: 269 → 273 (+4, DPairingMessages 重 + ContactGroups); shared/androidMain: 40 → 141 (+101, 含 chat 全 26 + 链上 deps ~75); **app/chat/ 0 文件，app/discover/ 0 文件**；**app/web/models/ 12 文件留 (Android-only)**。|
| 2026-06-27 | 累计进度 (Phase 15-19) | shared/commonMain: 269 → 273 (+4) ; shared/androidMain: 40 → 141 (+101) ; shared/iosMain: 11 → 11 (0) ; app/src/main: 1133 → 1027 (-106) ; **净迁移 +105 文件 (4 轮累加: Phase 15-16 +5, Phase 18 +18, Phase 19 +82)** |

| 2026-06-27 | **用户目标**：Migrate 大部分代码特别是 ui/ 目录，让 iOS app 直接用同样界面（隐藏 Android-only 功能）。当前基线：app/ 1027 文件 / shared/ 425 文件 (29%)。本规划拆为 Phase 20-26。 |

## Phase 20 — Platform Abstraction Layer (PAL) expect/actual 化

**目标**：把 285 处 `MainActivity.instance` / 124 处 `LocalContext.current` / 13 处 Intent / 5 处 Service / 5 处 WebView / 6 处 PdfRenderer / 3 处 PowerManager 等 Android-only API 全部封装成 `expect/actual`，让 ui/ 文件可以无修改搬到 shared。

**新增 expect/actual 清单**（集中在 `shared/.../platform/`）：

| expect fun | androidMain actual | iosMain actual | 替换的 Android-only API |
|------------|--------------------|-----------------|-------------------------|
| `expect fun showToast(text: String, long: Boolean = false)` | `Toast.makeText(...).show()` | UIAlertController 简短弹窗 | Toast |
| `expect fun launchUrl(url: String)` | `Intent(ACTION_VIEW).startActivity()` | `UIApplication.openURL` | Intent ACTION_VIEW |
| `expect fun shareText(text: String)` | `Intent(ACTION_SEND)` + `chooser` | `UIActivityViewController` | Intent ACTION_SEND |
| `expect fun shareFiles(paths: List<String>)` | FileProvider + Intent | `UIActivityViewController` | FileProvider |
| `expect fun pickFile(type: String): Flow<File?>` | SAF `ACTION_OPEN_DOCUMENT` | `UIDocumentPickerViewController` | SAF |
| `expect fun pickFiles(types: List<String>): Flow<List<File>>` | SAF multi | UIDocumentPicker multi | SAF multi |
| `expect fun pickFileOutput(name: String, type: String): Flow<File?>` | SAF `ACTION_CREATE_DOCUMENT` | `UIDocumentPicker` export | SAF create |
| `expect fun registerScreenCapture(): Flow<Bitmap>` | `MediaProjectionManager` | `ReplayKit.RPScreenRecorder` | MediaProjection |
| `expect fun requirePermission(p: Permission): Flow<Boolean>` | `ActivityResultContracts` | 自定义 iOS permission handler | Android Activity result |
| `expect fun isPermissionGranted(p: Permission): Boolean` | `ContextCompat.checkSelfPermission` | 系统 API | checkSelfPermission |
| `expect fun openAppSettings()` | `Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)` | `UIApplication.openSettingsURLString` | App settings |
| `expect fun wakeLock(timeoutMs: Long): WakeLock` | `PowerManager.newWakeLock` | `UIApplication.beginBackgroundTask` | PowerManager |
| `expect fun getBatteryLevel(): Int` | `BatteryManager` | `UIDevice.current.batteryLevel` | Battery |
| `expect fun getNetworkType(): NetworkType` | `ConnectivityManager` | `NWPathMonitor` | ConnectivityManager |
| `expect fun isBluetoothEnabled(): Boolean` | `BluetoothAdapter` | `CBCentralManager.state` | Bluetooth |
| `expect fun isCameraAvailable(): Boolean` | `PackageManager.hasFeature(FEATURE_CAMERA)` | `AVCaptureDevice.authorizationStatus` | Camera |
| `expect fun capturePhoto(): File?` | `ActivityResultContracts.TakePicture` | `UIImagePickerController` | Camera intent |
| `expect fun getDeviceName(): String` | `Build.MODEL` + `Build.MANUFACTURER` | `UIDevice.current.name` | Build |
| `expect fun getInstallSource(): String` | `packageManager.getInstallerPackageName` | bundle URL | Installer |
| `expect fun MainActivity(): Any?` | `MainActivity.instance` | `null` (UIViewController) | MainActivity.instance |
| `expect fun getBuildType(): BuildType` (Fdroid/Github/Google × debug/release) | `BuildConfig.BUILD_TYPE + FLAVOR` | `Bundle.main.bundleIdentifier` | BuildConfig |

**核心 trick**:
- `MainActivity.instance` 5 处 → `expect fun MainActivity(): Any?` (androidMain returns activity, iosMain returns null). 在需要 `ComponentActivity` 的地方用 `as? ComponentActivity ?: null`.
- 124 处 `LocalContext.current as Context` → 已经有 shared `appContext` (Phase 18 AppContext.kt), 优先用 `appContext`, 必要时 `LocalContext.current`.
- `BuildConfig` 在 shared 不可用 → Phase 19 已经在 `getAppVersion()` 用 `appContext.packageManager.getPackageInfo()`, `getBuildType()` 同理.
- `expect/actual` 不能跨 platform 修改 `MainActivity.instance` 这种静态字段 → 必须替换调用方代码, 不能 hook.

**新建文件**:
- `shared/commonMain/.../platform/PlatformAbstractions.kt` (35+ expect fun)
- `shared/commonMain/.../platform/FilePicker.kt`
- `shared/commonMain/.../platform/Permissions.kt`
- `shared/commonMain/.../platform/Share.kt`
- `shared/commonMain/.../platform/ScreenCapture.kt`
- `shared/commonMain/.../platform/AppInfo.kt`
- `shared/commonMain/.../platform/Battery.kt`
- `shared/commonMain/.../platform/Power.kt`
- `shared/commonMain/.../platform/Bluetooth.kt`
- `shared/commonMain/.../platform/Camera.kt`
- `shared/commonMain/.../platform/Connectivity.kt`
- `shared/androidMain/.../platform/Platform.android.kt` (所有 actual)
- `shared/iosMain/.../platform/Platform.ios.kt` (所有 actual)

**预计改动**: 1 个 PlanEntry + ~12 个新 shared 文件 + ~285 处 MainActivity.instance 替换 + ~50 处 Intent 替换.

**Build 验证**: 全绿 + 各 expect/actual 都有 androidMain + iosMain 实现.

---

## Phase 21 — UI base 第三方依赖子目录 expect/actual 化 (61 个 .kt → shared)

**目标**：app/ui/base/ 第三方子目录 (61 个 .kt) 全部搬到 shared/commonMain/ui/base/ 对应子目录.

| 子目录 | .kt 数 | Android-only 依赖 | 跨平台方案 |
|--------|--------|-------------------|------------|
| `coil/` | 3 | Coil 2.x (AsyncImage) | `expect fun AsyncImage(model: Any, ...)` — androidMain 用 Coil, iosMain 用 `UIImage` + Kingfisher 或 Ktor HTTP client |
| `colorpicker/` | 18 | 纯 Compose + Canvas | 全部搬到 shared/commonMain (无 Android-only) |
| `fastscroll/` | 16 | 纯 Compose LazyColumn/Grid | 全部搬到 shared/commonMain |
| `markdowntext/` | 3 | `compose-markdown` | 用 multiplatform-markdown-renderer (jetbrains/compose) |
| `mdeditor/` | 10 | Compose TextField + markdown live preview | 自己实现: shared/commonMain `MDEditorTextField` + markdown preview |
| `pullrefresh/` | 11 | Material3 PullToRefresh | Material3 1.3+ 已经有跨平台 PullToRefresh, 直接搬 |

**核心 trick**:
- Coil 2.x 不支持 iOS KMP target. **方案 1**: 用 expect/actual 包 AsyncImage, androidMain = Coil, iosMain = 简单 Box + `Painter` (用 Ktor 下载图片到本地 cache). **方案 2**: 换 multiplatform Coil 3.x. 选 2 (改造成本低).
- `colorpicker/` 18 个 .kt 看起来多但都是 HsvColorPicker 的派生组件 + state class, 纯 Compose 无 Android API, 应该全部 commonMain.
- `fastscroll/` 16 个 .kt 是 LazyColumn fast scroll thumb + 状态管理, 纯 Compose, 全 commonMain.
- `markdowntext/` 用 jetbrains 官方 `org.jetbrains.compose.markdown` (KMP-ready) 替换.

**app/ui/base 顶层 10 个简单文件**:
- `AceEditor.kt`, `ClipboardCard.kt`, `CopyIconButton.kt`, `MediaPageTitle.kt`, `MediaTopBar.kt`, `MinimalScrollHandle.kt`, `NeedPermissionColumn.kt`, `PDonationBanner.kt`, `PdfView.kt`, `TextLinkActions.kt` → 检查后大部分搬到 shared (PdfView 用 multiplatform-pdf-renderer, NeedPermissionColumn 用 Phase 20 `requirePermission`).
- `PdfView.kt`: 用 expect/actual PdfRenderer (androidMain = `android.graphics.pdf.PdfRenderer`, iosMain = PDFKit `PDFDocument`).

**预计改动**: 71 个 .kt 文件全部到 shared.

---

## Phase 22 — UI components 全迁移 (81 → 0)

**目标**：app/ui/components/ 81 个文件全搬到 shared/commonMain/ui/components/.

**分批** (按依赖深度):

### Phase 22.1 — 纯 Compose (28 个, 无 Android-only)
- `AddToHomeDialog.kt`, `ColorPickerDialog.kt`, `DeviceRenameDialog.kt`, `FileRenameDialog.kt`, `FileSortDialog.kt`, `FolderKanbanDialog.kt`, `FolderKanbanOptions.kt`, `FeedEntryListItem.kt`, `FeedListItem.kt`, `ImageMetaRows.kt`, `MediaFilesSelectModeBottomActions.kt`, `MediaFolderGridItem.kt`, `MediaFolderListItem.kt`, `NoteListItem.kt`, `SortAndBrowseDialog.kt`, `TagSelector.kt`, `VideoMetaRows.kt`, `WebAddressBarActions.kt`, `WebAddressBarEditDialogs.kt`, `WebAddressBarRow.kt`

### Phase 22.2 — Coil 依赖 (5 个, 等 Phase 21.1 完成)
- `DocItem.kt`, `ImageGridItem.kt`, `PackageListItem.kt`, `VideoGridItem.kt` (用 `AsyncImage` from Phase 21.1)

### Phase 22.3 — Intent/Activity 依赖 (10 个, 用 Phase 20 PAL)
- `QrScanResultBottomSheet.kt` (相机 → Phase 20 `capturePhoto`)
- `WebAddressBar.kt`, `WebAddressBarQrDialog.kt` (扫码 → Phase 20)
- 其余用 `launchUrl` / `shareText` 替换

### Phase 22.4 — WebView 依赖 (5 个, expect/actual WebView)
- `EditorWebViewClient.kt`, `EditorWebViewInterface.kt` → expect/actual `WebView` (androidMain = `android.webkit.WebView`, iosMain = `WKWebView` via `UIKit.UIViewControllerRepresentable`)

### Phase 22.5 — mediaviewer/ (子目录)
- 17 个左右文件, 纯 Compose, 全搬.

**预计改动**: 81 → 0.

---

## Phase 23 — UI page 全迁移 (184 → 0)

**目标**：app/ui/page/ 184 个文件全搬到 shared/commonMain/ui/page/.

**Hide Android-only 模式**:
```kotlin
@Composable
fun SomePage() {
    if (isAndroidOnly()) {
        AndroidOnlyFeature()
    }
    // 全平台 feature
    CommonFeature()
}
```

**分批** (按 feature 域):

| Phase | 子目录 | 文件数 | 关键 trick | 风险 |
|-------|--------|--------|-----------|------|
| 23.1 | settings/ | 9 | `BuildConfig.BUILD_TYPE` → `getBuildType()`, `ComponentShowcasePage` 直接搬 | low |
| 23.2 | home/ | 20 | `OnlineSessionsIndicator` 用 Ktor client (shared), `UpdateBanner` 用 `appContext.packageManager` | medium |
| 23.3 | chat/ | 37 | `ChatPage` 最大 page 之一, 用 Phase 19 ChatManager (已 shared), WebSocketHelper (shared), `TopBarChat` 部分用 Android-only 菜单 (hide) | high |
| 23.4 | tools/ | 2 | `SoundMeterRecorder` 用 `getAudioRecord()` expect/actual (androidMediaRecorder / iOS AVAudioRecorder) | medium |
| 23.5 | files/ | 15 | `FileActionsHelper` 用 Phase 20 Intent helpers, 子 components 用 Coil | medium |
| 23.6 | media/ (含 audio 22 + images 7 + docs 7 + cast 8 + dlna 7 + scan 5) | 56 | audio/video 用 Media3 expect/actual (androidMain = Media3 ExoPlayer, iosMain = AVPlayer); cast/DLNA iOS hide | high |
| 23.7 | pomodoro/ | 7 | `PomodoroHelper` 用 NotificationManager (Phase 20 PAL) | medium |
| 23.8 | feeds/ | 15 | `FeedWorkerStatus` 已迁 shared (Phase 15) | medium |
| 23.9 | connections/ | 6 | Bluetooth 用 Phase 20 `isBluetoothEnabled()`, Nearby 用已经迁的 `NearbyDiscoverManager` | medium |
| 23.10 | appfiles/ | 5 | 用 Phase 20 file picker | low |
| 23.11 | devoptions/ | 1 | `WebDevPage` 用 WebView (Phase 22.4) | low |
| 23.12 | tools 外的杂项 | ~2 | `TextFilePage`, `OtherFilePage`, `ViewTextContentBottomSheet`, `CrashReportDialog` | low |

**预计改动**: 184 → 0.

---

## Phase 24 — ViewModel 全迁移 (17 → 0)

**目标**: app/src/main/java/com/ismartcoding/plain/ui/models/ 下 26 个 ViewModel, shared 9 个已迁 (Files/Notes/Note/Tags/FeedEntry/ScanHistory/Update + ISelectable/ISearchable interface), 剩 17 个.

**chain move 模式**: 每个 VM 5-15 个 chain 文件, 按 FilesViewModel 模式 (Phase 18.1-18.3) 走.

| VM | chain 文件数 | 关键依赖 | 估计工作量 |
|----|--------------|----------|------------|
| MainViewModel | ~5 | HttpServerManager (Android-only Ktor), Permission | 2h |
| ChatViewModel + ChannelViewModel + PeerViewModel | ~3 | ChatManager/ChannelManager/PeerManager (Phase 19 已 shared) | 1h |
| AppsViewModel + NotificationSettingsViewModel | ~3 | DPackage (Android-only), PackageHelper | 1h |
| AudioViewModel + AudioPlaylistViewModel + VideosViewModel + ImagesViewModel + DocsViewModel | ~10 | DAudio/DPlaylistAudio (Phase 19 留 app/Media3-only), MediaStore | 4h |
| FeedsViewModel + FeedEntriesViewModel + FeedSettingsViewModel | ~5 | FeedEntryHelper (已 shared) | 1.5h |
| PomodoroViewModel | ~3 | PomodoroHelper (Notification + MediaPlayer) | 1.5h |
| SessionsViewModel + WebConsoleViewModel | ~5 | HttpServerManager (Android-only) | 2h |
| CastViewModel + DlnaReceiverViewModel | ~5 | CastPlayer/DLNA receiver (Android-only) | 2h |
| NearbyViewModel | ~3 | Bluetooth + NearbyDiscoverManager (Phase 19 已 shared) | 1h |
| MediaFoldersViewModel + TextFileViewModel | ~5 | MediaStore + FileSystemHelper (shared) | 1.5h |
| BackupRestoreViewModel | ~3 | ZipHelper + FileSystemHelper (shared) | 1h |
| MdEditorViewModel | ~3 | mdeditor (Phase 21 跨平台化) | 1h |

**iOS hide 策略**: Android-only 依赖的 VM 在 iOS 上不实例化, ViewModelProvider 用 `if (isAndroidOnly()) { RealVM() } else { StubVM() }`.

**预计改动**: 17 VM → 0 (剩 stub 给 iOS).

---

## Phase 25 — iOS 适配 + 业务逻辑跨平台化

**目标**: 让 iOS app 真正能 build + run + 用大部分功能.

**现状**: iosApp/iosApp/ContentView.swift 用 `UIViewControllerRepresentable` 包 `SharedAppNavHostKt.SharedAppNavHost()`, 已能跑 Compose UI.

**iOS 必须有的适配**:

1. **MainApp.onCreate 适配** (app/MainApp.kt onCreate 140 行)
   - 拆 expect/actual: `expect fun initApp()`, `expect fun initHttpServer()`, `expect fun initNotification()`, `expect fun initDatabase()`, `expect fun initLogging()`
   - androidMain actual 跑 MainApp.onCreate 全部逻辑
   - iosMain actual 跑 ios-equivalent (init AppDatabase + Logging + Channel 事件订阅)

2. **Service 适配** (app/services/ 8 文件)
   - `AudioPlayerService`, `HttpServerService`, `ScreenMirrorService`, `PNotificationListenerService`, `PlainAccessibilityService`, `QSTileService` → 全部 Android-only, iosMain actual = 空操作
   - 用 Phase 20 `expect fun startService(s: ServiceType): Boolean`

3. **BroadcastReceiver 适配** (app/receivers/ 5 文件)
   - `BatteryReceiver`, `NetworkStateReceiver`, `PeerChatReplyReceiver`, `PlugInControlReceiver`, `ServiceStopBroadcastReceiver` → Android-only
   - 用 Phase 20 `expect fun registerReceiver(...)`, iosMain actual = iOS NotificationCenter

4. **MainActivity.kt** (app/ui/MainActivity.kt)
   - Android-only (ComponentActivity 子类)
   - iosApp 直接用 Compose, 不需要 MainActivity.kt

5. **AppFeature visibility**:
   - 在 NavHost 注册路由时, 用 `if (isAndroidOnly()) composable("feature_x") { ... }`
   - iOS nav drawer / home page 不显示 Bluetooth, DLNA, Cast, Screen Mirror, SMS, Call, Notification Listener, Android Auto 等菜单项

6. **kmmbridge / xcframework**:
   - shared/build.gradle.kts 加 `XCFramework` config, 用 kmmbridge 或直接 `embedAndSignAppleFrameworkForXcode`
   - iosApp.xcodeproj 配置 framework search path

7. **iOS-specific 修复**:
   - iOS 不支持 Compose 的 `Modifier.windowInsetsPadding(WindowInsets.systemBars)` → 用 `safeContentPadding()` 替代
   - iOS keyboard 不触发 `WindowInsets.ime` → 自己 implement keyboard listener
   - iOS 文件路径 sandbox 限制 → expect/actual `getDocumentDir()` (androidMain = `filesDir`, iosMain = `NSDocumentDirectory`)
   - iOS 启动慢 → 用 baseline profile (Compose Multiplatform 1.6+ 支持)

**预计改动**: 
- 1 个新 shared/.../platform/Service.kt (expect/actual)
- 1 个新 shared/.../platform/Receiver.kt (expect/actual)
- 1 个新 shared/.../platform/AppInit.kt (expect/actual)
- iosApp/iosApp/Info.plist 加权限说明 (NSCameraUsageDescription, NSBluetoothAlwaysUsageDescription, NSPhotoLibraryUsageDescription)
- iosApp.xcodeproj 改 framework 引用

**Build 验证**: `xcodebuild -project iosApp.xcodeproj -scheme iosApp build` 全绿.

---

## Phase 26 — 收尾 + 性能调优 + 文档

1. **全 build 验证**:
   - `:shared:compileCommonMainKotlinMetadata` ✓
   - `:app:assembleFdroidDebug :app:assembleGithubDebug :app:assembleGoogleDebug` ✓
   - `xcodebuild -project iosApp.xcodeproj -scheme iosApp build` ✓

2. **启动速度优化**:
   - iOS: Compose Multiplatform baseline profile (kotlin 1.9.20+ 支持, 启动时间从 ~3s 降到 ~1s)
   - Android: startup profile (Macrobenchmark + BaselineProfileRule)

3. **内存优化**:
   - iOS: 检查 Compose recomposition scope, 用 `derivedStateOf` 缓存
   - 跨平台: 用 `key()` block 避免无谓重组

4. **跨平台架构文档**:
   - `docs/kmp-architecture.md` (新建): 说明 shared/app/iosApp 边界, expect/actual 列表, Android-only 留 app/ 的模块
   - 更新 `README.md` 加 iOS build 说明

5. **总结 entry**:
   - 在 plan doc 加最终 entry: `| YYYY-MM-DD | Phase 20-26 完成 | shared/commonMain 273 → 600+ ; shared/androidMain 141 → 300+ ; shared/iosMain 11 → 80+ ; app/src/main 1027 → 100 (剩 MainActivity/MainApp + Android-only 模块) ; iosApp 从 stub 变 fully functional |`

**预计总改动**: Phase 20-26 全部完成后, 大约:
- shared/commonMain: 273 → 600+ (+327)
- shared/androidMain: 141 → 300+ (+159)
- shared/iosMain: 11 → 80+ (+69)
- app/src/main: 1027 → 100 (-927)
- **净迁移**: app/ 90% 内容搬到 shared, 剩 100 个 Android-only 模块留 app/

**风险评估**:
- **high**: Phase 23.3 (chat/), Phase 23.6 (media/) — 最大 page, 最多 Android-only 依赖
- **medium**: Phase 22.4 (WebView expect/actual), Phase 24 (audio/video VM)
- **low**: 其它

**总时间预估**: Phase 20 (1 周) + Phase 21 (3 天) + Phase 22 (1 周) + Phase 23 (3 周, 大头) + Phase 24 (1 周) + Phase 25 (1 周) + Phase 26 (3 天) = **~8 周**

| 2026-06-27 | Phase 20 — Platform Abstraction Layer (PAL) | **6 个 expect/actual 模块, 13 个 expect fun, 5 处 Intent 调用替换**。<br>**新建**：`shared/.../platform/` 下 6 个 commonMain + 6 个 androidMain + 6 个 iosMain 文件：<br>- `AppInfo` (4 fun: getAppVersion/getOSVersion/getDeviceName/getBuildType)<br>- `Launcher` (4 fun: launchUrl/openAppSettings/shareText/shareFile)<br>- `Permission` (1 fun: isPermissionGranted)<br>- `Network` (1 fun: getNetworkType + NetworkType enum)<br>- `Bluetooth` (2 fun: isBluetoothEnabled/isBluetoothSupported)<br>- `Battery` (1 fun: getBatteryLevel)<br>**修 AppContext.kt**: 加 `buildTypeValue: String` 参数, `setAppContext(context, buildType)`。<br>**替换 5 处 Intent** (8 个修改点):<br>- `ChatLinkPreview.kt` ACTION_VIEW → `launchUrl`<br>- `ChatLinkPreviewStates.kt` ACTION_VIEW → `launchUrl`<br>- `WebHelper.kt` ACTION_VIEW → `launchUrl`<br>- `TextLinkActions.kt` SENDTO/ACTION_DIAL → `launchUrl("mailto:")`/`launchUrl("tel:")`<br>- `WebSettingsPage.kt` ACTION_APPLICATION_DETAILS_SETTINGS → `openAppSettings()`<br>**推迟 (留 Phase 22/25)**:<br>- `requirePermission` / `pickFile*` (Activity launcher, 需 UIViewController 桥接)<br>- `capturePhoto` / `registerScreenCapture` (MediaProjection/ReplayKit)<br>- `wakeLock` / `MainActivity()` accessor<br>- MainActivity.kt / MainActivityEvents.kt / MainActivityIntentHandler.kt / PomodoroHelper.kt / CrashReportDialog.kt 的 Intent (Activity 本身或附件逻辑复杂)<br>**iOS 占位**: shareText/shareFile 在 iosMain no-op (Phase 25 桥接 UIViewController)。<br>**Build 验证**: Android 3 flavor 全绿, iOS commonMain/iosMain compile OK (KSP @Database error 是 Phase 19 pre-existing, Phase 25 修)。Tests 74/77 pass (3 pre-existing macOS-only)。<br>**Uncommitted 文件** (user 自己 commit): 19 新 platform/ 文件 + 5 modified app/ 文件 + 1 modified AppContext.kt。MainApp.onCreate 还要改成 `setAppContext(this, "fdroid-debug")` 才能让 `getBuildType()` 返回正确值。 |

| 2026-06-27 | Phase 21 — UI base 第三方依赖迁移 (部分) | **3 个简单子目录全迁 + 顶层 2 个文件**。已迁 29/71 = 41%。<br>**完成迁移**:<br>- `pullrefresh/` (11 文件) — 纯 Compose, 全 commonMain。修 `RefreshLayoutState.kt` 的 `internal val` → `val` (跨 module 不可见, 同 Phase 19 `mePeer` 处理)<br>- `fastscroll/` (16 文件) — HapticFeedback 用 `LocalHapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)` 替换 `view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)`<br>- 顶层 `ClipboardCard.kt` + `CopyIconButton.kt` — `LocalClipboardManager.setText(AnnotatedString)` 替换 `clipboardManager.setPrimaryClip(ClipData.newPlainText(...))`<br>**留 app/** (依赖 Android-only 库/类):<br>- 顶层 7 个文件: `AceEditor` (WebView), `MediaPageTitle` (CastPlayer), `MediaTopBar` (BaseMediaViewModel+CastViewModel+FileSortBy), `MinimalScrollHandle` (PDFView+View), `NeedPermissionColumn` (Permission enum), `PDonationBanner` (Coil), `PdfView` (PdfRenderer)<br>- `coil/` (3) — Coil 3 KMP-ready 部分, 待 Phase 21.1 expect/actual `AsyncImage`<br>- `colorpicker/` (18) — 1214 行 android.graphics 重写为 Compose Graphics 工作量大, 待评估用 `compose-colorpicker` 第三方库替换<br>- `markdowntext/` (3) — `AndroidView` + `TextView` + `Spanned`, iOS 上 `AndroidView` 是 stub 不工作, 待用 `org.jetbrains.compose.markdown` 替换<br>- `mdeditor/` (10) — 整体依赖 `MdEditorViewModel` (Permission + inlineWrap), 待 Phase 24 VM 链迁<br>**Build 验证**: Android 3 flavor 全绿。<br>**下一步**: 评估 colorpicker 用 `compose-colorpicker` 库替换 (Phase 21.2) + markdowntext 用 jetbrains compose markdown 替换 (Phase 21.3) + 顶层 WebView/PdfView/Coil expect/actual (Phase 22.4 之后)。 |
