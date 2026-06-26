# PrettyFormat 简化版 — Disk Log 保留 method/thread 信息

修复 KMP 化 DiskLogFormatStrategy 时丢失的 caller method + thread 信息。

## 现状

LogCat 跨平台化（2026-06-27 完成）时，原 `PrettyFormatStrategy` 因依赖 `Thread.currentThread().stackTrace`（JVM-only）和 `Math.min` / `String.format` 等 JVM-only API 被删除。
原 disk log 行格式：
```
2026-06-27 01:04:28.123 DEBUG PlainApp ┌────────────────────────
                                    │ Thread: main
                                    ├┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄
                                    │ ChatFileStore.storedNewFile (ChatFileStore.kt:131)
                                    ├┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄
                                    │ stored new file abc123 (1024 bytes)
                                    └────────────────────────
```
新 disk log 行格式：
```
2026-06-27 01:04:28.123 DEBUG PlainApp stored new file abc123 (1024 bytes)
```
丢失了 method/thread 信息，调试体验下降。

## 方案：简化版 PrettyFormat（去边框，保留 method/thread）

新 disk log 行格式：
```
2026-06-27 01:04:28.123 DEBUG PlainApp │ [main] ChatFileStore.storedNewFile (ChatFileStore.kt:131) stored new file abc123 (1024 bytes)
```

每条 log 一行，前缀带：
- `│ [threadName]` —— 当前线程名（NSThread/Thread）
- `SimpleClass.method (file:line)` —— caller 信息

边框、`│ ` 多行 message 分割、CHUNK_SIZE 全部去掉。

## 实施步骤

- [x] 收集现状：读 `DiskLogFormatStrategy` / `FormatStrategy` / `LoggerPrinter` / 原 `PrettyFormatStrategy`
- [ ] commonMain: `expect fun currentThreadName(): String`
- [ ] androidMain: `Thread.currentThread().name`
- [ ] iosMain: `platform.Foundation.NSThread.currentThread().name ?: "main"`
- [ ] commonMain: `DiskLogFormatStrategy.log` 调用 `Throwable().stackTrace` 提取 caller
      - 过滤内部 frame: `Throwable.<init>`, `DiskLogFormatStrategy.log`, `DiskLogAdapter.log`, `LoggerPrinter.log`, `LogCat.*`
      - 取第一个非内部 frame 作 caller
      - 用 `SimpleClassName.method (fileName:line)` 输出
- [ ] 防御：stack empty / 找不到 caller → 不加前缀，原行输出
- [ ] Android 3 flavor assemble + commonMain compile 全绿