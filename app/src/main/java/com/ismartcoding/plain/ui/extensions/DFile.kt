package com.ismartcoding.plain.ui.extensions

import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.ui.components.mediaviewer.PreviewItem

fun DFile.toPreviewItem(): PreviewItem {
    return PreviewItem(path, path, size, path, this)
}