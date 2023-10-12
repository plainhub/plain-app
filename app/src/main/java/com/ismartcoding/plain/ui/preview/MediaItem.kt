package com.ismartcoding.plain.ui.preview

import android.content.Context
import android.net.Uri
import com.ismartcoding.lib.extensions.getFileName
import com.ismartcoding.lib.extensions.isVideoFast
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

data class PreviewItem(
    val id: String,
    val uri: Uri,
    val path: String = "",
) {
    fun itemType(context: Context): Int {
        return when {
            uri.getFileName(context).isVideoFast() -> ItemType.VIDEO
            else -> ItemType.IMAGE
        }
    }

    fun toInputStream(context: Context): InputStream? {
        return if (path.isNotEmpty()) FileInputStream(File(path)) else context.contentResolver.openInputStream(uri)
    }
}

object ItemType {
    const val UNKNOWN = -1
    const val IMAGE = 2
    const val VIDEO = 3
}
