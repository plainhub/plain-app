package com.ismartcoding.plain.platform

import android.content.ClipData
import android.content.ClipDescription
import android.os.PersistableBundle
import com.ismartcoding.plain.clipboardManager

actual fun setClipboardText(label: String, text: String, sensitive: Boolean) {
    val clip = ClipData.newPlainText(label, text)
    if (sensitive) {
        clip.description.extras = PersistableBundle().apply {
            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
        }
    }
    clipboardManager.setPrimaryClip(clip)
}

actual fun getClipboardText(): String? {
    return clipboardManager.primaryClip?.let { primaryClip ->
        if (primaryClip.itemCount > 0) {
            primaryClip.getItemAt(0)?.text?.toString()
        } else {
            null
        }
    }
}
