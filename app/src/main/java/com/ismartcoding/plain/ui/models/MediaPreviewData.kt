package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.ui.unit.toSize
import com.ismartcoding.lib.extensions.getFinalPath
import com.ismartcoding.lib.extensions.isImageFast
import com.ismartcoding.lib.extensions.isVideoFast
import com.ismartcoding.plain.data.DImage
import com.ismartcoding.plain.data.DVideo
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.db.DMessageImages
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.TransformItemState
import com.ismartcoding.plain.ui.components.mediaviewer.PreviewItem

object MediaPreviewData {
    var items = listOf<PreviewItem>()

    fun setDataAsync(
        context: Context,
        itemState: TransformItemState,
        chatItems: List<VChat>,
        m: DMessageFile
    ) {
        val newItems = mutableListOf<DMessageFile>()
        chatItems.forEach { item ->
            if (item.value is DMessageImages) {
                newItems.addAll((item.value as DMessageImages).items)
            } else if (item.value is DMessageFiles) {
                newItems.addAll((item.value as DMessageFiles).items.filter { it.uri.isVideoFast() || it.uri.isImageFast() })
            }
        }
        items = newItems.map { f ->
            PreviewItem(f.id, f.uri.getFinalPath(context), f.size, data = f)
        }
        items.find { it.id == m.id }?.let {
            it.initAsync(m)
            itemState.intrinsicSize = it.intrinsicSize.toSize()
        }
    }

    fun setDataAsync(
        itemState: TransformItemState,
        items: List<DImage>,
        m: DImage
    ) {
        MediaPreviewData.items = items.map { f ->
            PreviewItem(f.id, f.path, f.size, mediaId = f.id, data = f)
        }
        MediaPreviewData.items.find { it.id == m.id }?.let {
            it.initAsync(m)
            itemState.intrinsicSize = it.intrinsicSize.toSize()
        }
    }

    fun setDataAsync(
        itemState: TransformItemState,
        items: List<DVideo>,
        m: DVideo
    ) {
        MediaPreviewData.items = items.map { f ->
            PreviewItem(f.id, f.path, f.size, mediaId = f.id, data = f)
        }
        MediaPreviewData.items.find { it.id == m.id }?.let {
            it.initAsync(m)
            itemState.intrinsicSize = it.intrinsicSize.toSize()
        }
    }
    
    fun setDataAsync(
        context: Context,
        itemState: TransformItemState,
        previewItems: List<PreviewItem>,
        selectedItem: PreviewItem
    ) {
        MediaPreviewData.items = previewItems
        items.find { it.id == selectedItem.id }?.let {
            if (it.path.isImageFast()) {
                it.initImageAsync()
            }
            itemState.intrinsicSize = it.intrinsicSize?.toSize()
        }
    }
}