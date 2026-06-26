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