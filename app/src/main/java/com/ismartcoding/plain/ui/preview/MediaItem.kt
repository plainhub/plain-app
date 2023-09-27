package com.ismartcoding.plain.ui.preview

import android.content.Context
import android.net.Uri
import com.ismartcoding.lib.extensions.getFileName
import com.ismartcoding.lib.extensions.isVideoFast

data class PreviewItem(
    val id: String,
    val uri: Uri,
) {
    fun itemType(context: Context): Int {
        return when {
            uri.getFileName(context).isVideoFast() -> ItemType.VIDEO
            else -> ItemType.IMAGE
        }
    }
}

object ItemType {
    const val UNKNOWN = -1
    const val IMAGE = 2
    const val VIDEO = 3
}
