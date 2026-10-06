package com.ismartcoding.plain.ui.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun rememberClipboardWriter(): (String, String) -> Unit {
    val context = LocalContext.current
    val manager = remember(context) {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
    return remember(manager) {
        { label, text -> manager.setPrimaryClip(ClipData.newPlainText(label, text)) }
    }
}
