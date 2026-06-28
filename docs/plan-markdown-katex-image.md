# Markdown KMP 迁移后 katex / image 渲染修复

## 现状盘点

### 1. Markdown 库结构

| 层 | 位置 | 状态 |
|---|---|---|
| KMP core | `shared/src/commonMain/.../lib/markdown/` (compose/elements, coil3, model, annotator, utils) | 已迁 |
| KMP android | `shared/src/androidMain/.../lib/markdown/utils/MarkdownLogger.android.kt` | 已迁 |
| KMP iOS | `shared/src/nativeMain/.../lib/markdown/utils/MarkdownLogger.native.kt` | 已迁 |
| App 桥接层 | `app/src/main/.../ui/base/markdowntext/MarkdownText.kt` | **保留(必要)** |
| app/build.gradle.kts 里的 markwon 依赖 | — | **无残留** ✓ |

### 2. MarkdownText.kt 性质判断

**既不是漏迁移，也不是多余文件**——它是 app 层桥接层，承载 KMP 端 commonMain 没法做的工作：

- `AppImageTransformer` 解析 `app://` 和 `fid:` 协议 → 调 Android-only 扩展 `String.getFinalPath(Context)`，转绝对路径喂给 Coil3
- `linkInteractionListener` 接 `LinkAnnotation.Url`，判断是否是 image 链接（用本地正则 `IMAGE_MARKDOWN_REGEX` 抓所有 `![...](...)`），图片就开 `MediaPreviewerState`，普通链接就 `WebHelper.open(context, url)`
- `previewerState` 参数是 `MediaPreviewerState`（app 层 UI 状态）
- 配 Material 3 颜色 / typography 给 KMP 端 `MarkdownColors` / `MarkdownTypography`
- 拦截 GFM `BLOCK_MATH` / `INLINE_MATH` 节点走 `RenderMathNode`（`custom` lambda）

如果 user 想把它也搬到 KMP，得拆出 `imageTransformer` 协议层（按平台 expect/actual 拆 `app://`/`fid:`）+ `linkInteractionListener`（按平台拆 Intent / WebView 打开），工作量大且**没有跨平台价值**（feed/note 是 app-only 业务）。**保留**。

### 3. 依赖

```toml
# gradle/libs.versions.toml
latex = "1.3.0"
jetbrainsMarkdown = "0.7.5"
coil = "3.4.0"
```

```kotlin
// shared/build.gradle.kts
api(libs.jetbrains.markdown)
api(libs.latex.base)
api(libs.latex.parser)
api(libs.latex.renderer)
```

KMP markdown 库（`lib/markdown/`）vendored 进 `:shared` 模块。LaTeX 渲染用 huarangmeng/latex 1.3.0（Compose Multiplatform 原生 Canvas 渲染，**不依赖 webview**）。

### 4. 两个 bug 的初判

#### KaTeX 不渲染
- 路径：`MarkdownText.kt:custom` → `RenderMathNode` → `MarkdownMath` → `Latex` (huarangmeng)
- 已知：1.3.0 `LatexConfig` 字段是 `color` + `darkColor`（不是新版的 `theme = LatexTheme.auto()`），项目里用法**与 1.3.0 jar API 一致**。
- 字体资源：`latex-renderer.aar` 里有 `katex_*.ttf` 和 `latinmodern-math.otf` 在 `assets/composeResources/.../font/`，KMP 库通过 `api(libs.latex.renderer)` 暴露，AAR 资源会自动合并到 APK。
- `Latex` 内部用 `Canvas` 渲染 + 异步 `IncrementalLatexParser`（`LaunchedEffect(latex)`），首次组合 `document = LatexNode.Document(emptyList())` → `canvasSize = 0` → 看不见，**但异步解析完后会重组**。
- **真凶未定位** — 必须先 build + mobile-mcp 跑起来，看实际现象（颜色 / 大小 / 整块消失 / 渲染了但被裁掉）。

#### Image 不渲染
- 路径：`MarkdownImage` 调 `LocalImageTransformer.current.transform(link)` → `AppImageTransformer.transform()` → Coil3 painter
- 注意：`AppImageTransformer.transform()` 返回的是 `ImageData(painter, contentDescription)`，**没有传 `modifier`**——`MarkdownImage` 内部会 chain `imageData.modifier`，但 `MarkdownImage` 自己已经用 `Box(... fillMaxWidth().widthIn(min=48.dp).heightIn(min=48.dp).then(imageData.modifier))` 兜底。
- `AppImageTransformer` 用了 `coil3.size.Size.ORIGINAL`（保持原图分辨率）+ `coil3.compose.rememberAsyncImagePainter`。
- **真正的不渲染原因未定位** —— 必须先 build 跑起来，看是不是 `app://`/`fid:` 解析失败 / Coil3 加载失败 / image 节点根本进不了 `MarkdownImage`。

## Plan

- [x] 1. 收集信息、盘点结构、判断 MarkdownText.kt 性质
- [ ] 2. `./gradlew :app:assembleDebug` + `./gradlew :app:installDebug`（不主动 install，先 adb 看下设备——本任务**必须 user 显式 install 才 install**，按 memory 规则）
- [ ] 3. mobile-mcp 跑起来 → 打开笔记 / feed → 写一条带 `$$x^2 + y^2 = z^2$$` 和 `![alt](app://...)` / `![alt](fid:...)` / `![alt](https://...)` 的内容
- [ ] 4. 截图 + logcat → 定位 katex / image bug
- [ ] 5. 按症状修（不在改之前先猜）—— 修完立即翻 [x] + 落地注
- [ ] 6. mobile-mcp 再截图 + logcat 验证

## Scope 纪律

- 只改 `lib/markdown/` (KMP core) + `app/.../MarkdownText.kt` 渲染相关代码
- 不动 Markwon 历史注释 / 旧 markwon 相关引用（已经是注释）
- 不抽 composable / 不拆文件 / 不动 note/feed 业务代码
- 不重排 indent / 不动空行

## 风险

- huarangmeng/latex 1.3.0 在 dark mode 下的字色 / 大小如果不对，可能需要切到 `LatexConfig(theme = LatexTheme.material3())`（**但 1.3.0 还不支持 theme 字段**，要 theme 字段得升 1.4.7+ —— 先观察再说）
- Compose Multiplatform resources 在 AAR 里的合并，可能需要 `androidResources.localeFilters` 配置（先不预设，先 build 看 APK 是不是含了 ttf）
