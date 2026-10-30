package com.ismartcoding.plain.ui.media

import com.ismartcoding.plain.ui.chat.VChat

import com.ismartcoding.plain.models.Video as MediaVideo
import com.ismartcoding.plain.models.Image as MediaImage
import androidx.compose.ui.unit.toSize
import com.ismartcoding.plain.features.chat.models.MessageFile
import com.ismartcoding.plain.features.chat.models.MessageFiles
import com.ismartcoding.plain.features.chat.models.MessageImages
import com.ismartcoding.plain.models.Image
import com.ismartcoding.plain.models.Video
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.extensions.isVideoFast
import com.ismartcoding.plain.ui.media.viewer.PreviewItem
import com.ismartcoding.plain.ui.media.viewer.previewer.TransformItemState
import com.ismartcoding.plain.features.files.FilePaths

object MediaPreviewData {
    var items = listOf<PreviewItem>()

    suspend fun setDataAsync(
        itemState: TransformItemState,
        chatItems: List<VChat>,
        m: MessageFile,
        source: com.ismartcoding.plain.rust.graphql.ContentSource = com.ismartcoding.plain.rust.graphql.ContentSource.Local,
    ) {
        val newItems = mutableListOf<MessageFile>()
        chatItems.forEach { item ->
            if (item.value is MessageImages) {
                newItems.addAll((item.value as MessageImages).items)
            } else if (item.value is MessageFiles) {
                newItems.addAll((item.value as MessageFiles).items.filter { it.fileName.isVideoFast() || it.fileName.isImageFast() })
            }
        }
        if (source is com.ismartcoding.plain.rust.graphql.ContentSource.Remote) newItems.removeAll { it.isRemoteFile() }
        val paths = if (source is com.ismartcoding.plain.rust.graphql.ContentSource.Local) FilePaths.resolveAll(newItems.map { it.uri }) else newItems.map { it.uri }
        items = newItems.mapIndexed { index, f ->
            PreviewItem(f.id, paths[index], f.size, data = f, source = source)
        }
        items.find { it.id == m.id }?.let {
            it.initAsync(m)
            itemState.updateIntrinsicSize(it.intrinsicSize.toSize())
        }
    }

    suspend fun setDataAsync(
        itemState: TransformItemState,
        items: List<MediaImage>,
        m: MediaImage,
        source: com.ismartcoding.plain.rust.graphql.ContentSource = com.ismartcoding.plain.rust.graphql.ContentSource.Local,
    ) {
        MediaPreviewData.items = items.map { f ->
            PreviewItem(f.id, f.path, f.size, mediaId = f.id, data = f, source = source)
        }
        MediaPreviewData.items.find { it.id == m.id }?.let {
            it.initAsync(m)
            itemState.updateIntrinsicSize(it.intrinsicSize.toSize())
        }
    }

    suspend fun setDataAsync(
        itemState: TransformItemState,
        items: List<MediaVideo>,
        m: MediaVideo,
        source: com.ismartcoding.plain.rust.graphql.ContentSource = com.ismartcoding.plain.rust.graphql.ContentSource.Local,
    ) {
        MediaPreviewData.items = items.map { f ->
            PreviewItem(f.id, f.path, f.size, mediaId = f.id, data = f, source = source)
        }
        MediaPreviewData.items.find { it.id == m.id }?.let {
            it.initAsync(m)
            itemState.updateIntrinsicSize(it.intrinsicSize.toSize())
        }
    }

    suspend fun setDataAsync(
        itemState: TransformItemState,
        previewItems: List<PreviewItem>,
        selectedItem: PreviewItem
    ) {
        MediaPreviewData.items = previewItems
        items.find { it.id == selectedItem.id }?.let {
            if (it.isVideo()) {
                it.initVideoAsync()
            } else if (it.isImage()) {
                it.initImageAsync()
            }
            itemState.updateIntrinsicSize(it.intrinsicSize.toSize())
        }
    }
}
