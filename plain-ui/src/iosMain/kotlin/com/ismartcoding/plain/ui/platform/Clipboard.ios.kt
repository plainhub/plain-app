package com.ismartcoding.plain.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIPasteboard

@Composable
internal actual fun rememberClipboardWriter(): (String, String) -> Unit =
    remember { { _, text -> UIPasteboard.generalPasteboard.string = text } }

@Composable
internal actual fun rememberClipboardReader(): suspend () -> String? =
    remember { { UIPasteboard.generalPasteboard.string } }
