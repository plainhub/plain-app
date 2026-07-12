package com.ismartcoding.plain.platform

import android.content.ClipData
import com.ismartcoding.plain.clipboardManager

actual fun setClipboardText(label: String, text: String) {
    val clip = ClipData.newPlainText(label, text)
    clipboardManager.setPrimaryClip(clip)
}
