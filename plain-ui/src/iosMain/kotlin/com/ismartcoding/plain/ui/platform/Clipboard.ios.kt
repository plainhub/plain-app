package com.ismartcoding.plain.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIPasteboard

/** UIPasteboard 没有 label 概念，与 plain-app 的 platform 层行为一致：忽略 label。 */
@Composable
internal actual fun rememberClipboardWriter(): (String, String) -> Unit =
    remember { { _, text -> UIPasteboard.generalPasteboard.string = text } }
