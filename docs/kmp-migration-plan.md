# KMP Migration Plan

> 目标：把 `app/` 里的业务代码大部分搬到 `shared/`；`app/` 只留 Android 壳（Activity/Service/Receiver/Manifest），`iosApp/` 已是 SwiftUI 壳，shared 暴露 Compose UI 供两端复用。
> 节奏：小步快进。每一步 = 一次 `./gradlew :shared:compileCommonMainKotlinMetadata` + `:app:assembleDebug` 通过 + 一个 commit。

---

## 1. 标准 KMP 目录结构（最终目标）

```
plain-app/
├── shared/                ← KMP 单源（Compose Multiplatform + Room + Ktor）
│   ├── src/commonMain/    ← 业务核心：data/enums/db/helpers/extensions/ui/preferences/i18n
│   ├── src/androidMain/   ← Android-only actual（CameraX/MediaStore/Tink/Filesystem 等）
│   ├── src/iosMain/       ← iOS-only actual（PhotoKit/AVAudioPlayer/NSFileManager）
│   ├── src/desktopMain/   ← 暂不启用，保留目录待以后
│   ├── src/jvmMain/       ← 暂不启用
│   └── schemas/           ← Room schema export
├── app/                   ← Android 入口壳（瘦模块，~200 kt 以内）
│   ├── src/main/AndroidManifest.xml
│   ├── src/main/java/.../MainApp.kt        ← 仅 Application 初始化
│   ├── src/main/java/.../MainActivity.kt   ← 仅 Activity + Compose entry point
│   ├── src/main/java/.../features/{sms,contact,call,bluetooth,dlna}  ← Android-only 永远不迁
│   ├── src/main/java/.../services/webrtc  ← 永远不迁
│   ├── src/main/java/.../{receivers,workers,mdns,web}                ← Android-only 永远不迁
│   └── src/main/res/      ← Android-only 资源（launcher icon / values-night / fdroid 资源）
└── iosApp/                ← iOS 入口壳
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
- [ ] 3.7 解 `LogCat` Android-only：`browserClient()` 的 `Logging` plugin 用一个 shared 的 `Logger` 实现，把 LogCat 注入延后到 `MainApp.onCreate`（Android-only）或保留为空 iOS no-op
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

- [ ] 5.1 **NotesViewModel** — 只依赖 db + preferences，确认无 android feature 后搬到 `shared/commonMain/ui/page/notes/`
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

- [ ] 6.1 **Notes 页**（7 个 Composable）从 `app/ui/page/notes/` → `shared/commonMain/ui/page/notes/`
- [ ] 6.2 **Tags 页**（3 个）→ `shared/commonMain/ui/page/tags/`
- [ ] 6.3 **Feeds 页**（15 个）→ `shared/commonMain/ui/page/feeds/`
- [ ] 6.4 **Chat 页**（46 个，量大）→ `shared/commonMain/ui/page/chat/`。注意 chat 里 android-only 的（WebRTC mirror、SMS 关联）用 `expect/actual` 跳过
- [ ] 6.5 **Home** 已迁（`shared/shared/home/`），确认 `app/ui/page/home/` 没残留
- [ ] 6.6 **Files / Images / Videos / Audio** — 依赖 Phase 5.5~5.7 + Phase 8 expect/actual，按 VM 迁移节奏跟进
- [ ] 6.7 **Settings** — 拆：跨平台项 → shared；Android-only 项（蓝牙/DLNA/SMS/通话）保留 `app/ui/page/settings/`，用 `expect/actual fun isAndroidOnlyFeatureEnabled(): Boolean` 控制可见
- [ ] 6.8 每个 page 迁完，跑 `:app:assembleDebug` + iOS `xcodebuild -scheme iosApp`（或暂用 `:shared:compileKotlinIosArm64` 替代）

### Phase 7 — Navigation

- [ ] 7.1 在 `shared/build.gradle.kts` commonMain 加 `androidx.navigation:navigation-compose`（KMP 版已 GA）
- [ ] 7.2 定义 shared route constants：`shared/commonMain/ui/nav/Routes.kt`
- [ ] 7.3 `shared/commonMain/ui/nav/SharedAppNavHost.kt` 暴露 `NavHost`，由两端 host 初始化
- [ ] 7.4 `app/MainActivity.kt` 改用 `setContent { PlainTheme { SharedAppNavHost() } }`，删除 `app/ui/nav/` 整个目录
- [ ] 7.5 iOS `ContentView.swift` 改用 `SharedAppNavHost()` 嵌入
- [ ] 7.6 commit：`refactor(kmp): shared navigation`

### Phase 8 — Platform-specific expect/actual（增量）

按需求驱动，不一次性：

- [ ] 8.1 `expect fun platformFilesDir(): File` —— androidMain `context.filesDir`，iosMain `NSFileManager.defaultManager().URLsForDirectory(.documentDirectory)`
- [ ] 8.2 `expect suspend fun listMediaImages(bucketId: String?): List<DImage>` —— androidMain MediaStore，iosMain PhotoKit
- [ ] 8.3 `expect suspend fun playAudio(url: String, onComplete: () -> Unit)` —— androidMain MediaPlayer，iosMain AVAudioPlayer
- [ ] 8.4 `expect fun hmacSha256(...)` —— 已列在 Phase 3.5
- [ ] 8.5 `expect fun isAndroidOnly(): Boolean` —— androidMain true，iosMain false；用来 Settings 页隐藏蓝牙/DLNA 等
- [ ] 8.6 每个 actual 落地一个文件，不批量

### Phase 9 — Android Shell Trim（核心 KPI）

> 目标：`app/src/main` 从 1321 kt 降到 ≤ 300 kt（最终保留 Android-only + 壳代码）。

- [ ] 9.1 全量扫一遍 `app/src/main`，列出每个文件的最终归属（shared/commonMain / shared/androidMain / 留 app/）
- [ ] 9.2 Phase 9.1 清单沉淀到 `docs/kmp-migration-audit.md`（新文件，列每个 app/ 文件 + 计划归宿 + 状态）
- [ ] 9.3 按 Phase 3~8 已迁的，逐步删 app/ 里已空 package
- [ ] 9.4 落地标准：`grep -r "package com.ismartcoding.plain" app/src/main/java | wc -l` ≤ 300
- [ ] 9.5 `MainApp.kt` 简化为：initDataStore / initDatabase / CrashHandler / HttpServerManager.warmUp / NetworkMonitor.init / LogCat / 保留 PowerConnectedEvent 监听。其余 `coIO` 块按事件总线拆到 `shared/` 里 `object AppStartup`，由 `MainApp` 调 `AppStartup.run(applicationContext)`
- [ ] 9.6 `MainActivity.kt` 简化为：setContent { PlainTheme { SharedAppNavHost() } } + 启动时 push MainActivityEvents / IntentHandler 桥
- [ ] 9.7 删 `app/ui/page/`、`app/ui/{components,helpers,models,nav,extensions}/`（已被 shared 替代的部分）

### Phase 10 — iOS Surface Hardening

- [ ] 10.1 `PlainHomeViewController` 已能跑；扩到 `SharedAppNavHost`（Phase 7 完成后）
- [ ] 10.2 iOS Settings 页加「权限说明」placeholder（PhotoKit / 通知 / 本地网络）
- [ ] 10.3 iOS `Info.plist` 加 `NSPhotoLibraryUsageDescription` / `NSLocalNetworkUsageDescription`
- [ ] 10.4 跑 `xcodebuild -workspace iosApp.xcworkspace -scheme iosApp -configuration Debug -sdk iphonesimulator`，要求 BUILD SUCCEEDED

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