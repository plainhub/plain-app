package com.ismartcoding.plain.ui.platform

import androidx.compose.runtime.Composable

/**
 * 拿一个写剪贴板的函数。返回 lambda 而不是直接写，是因为 `label` 只有走
 * `ClipData.newPlainText(label, text)` 才带得上——Compose 自带的
 * `LocalClipboardManager.setText()` 建的是无 label 的 ClipData，Android 13+
 * 的系统剪贴板浮层会退回显示 "Copied"。而 `onClick` 不是 @Composable 上下文，
 * 没法直接调 expect/actual，所以把「拿能力」和「用能力」拆开。
 */
@Composable
internal expect fun rememberClipboardWriter(): (label: String, text: String) -> Unit
