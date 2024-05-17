package com.ismartcoding.plain.ui.models

import android.content.Context
import android.net.Uri
import androidx.compose.ui.unit.toSize
import com.ismartcoding.lib.extensions.getFinalPath
import com.ismartcoding.plain.data.DImage
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.TransformItemState
import com.ismartcoding.plain.ui.preview.PreviewItem

object MediaPreviewData {
    var items = listOf<PreviewItem>()
    var index = 0

    fun setDataAsync(
        context: Context,
        itemState: TransformItemState,
        files: List<DMessageFile>,
        m: DMessageFile
    ) {
        items = files.map { f ->
            PreviewItem(f.id, Uri.EMPTY, f.uri.getFinalPath(context), f.size, data = f)
        }
        items.find { it.id == m.id }?.let {
            it.initAsync(context, m)
            itemState.intrinsicSize = it.intrinsicSize.toSize()
        }
    }

    fun setDataAsync(
        context: Context,
        itemState: TransformItemState,
        items: List<DImage>,
        m: DImage
    ) {
        MediaPreviewData.items = items.map { f ->
            PreviewItem(f.id, Uri.EMPTY, f.path, f.size, mediaId = f.id, data = f)
        }
        MediaPreviewData.items.find { it.id == m.id }?.let {
            it.initAsync(context, m.width, m.height)
            itemState.intrinsicSize = it.intrinsicSize.toSize()
        }
    }
}