package com.ismartcoding.plain.ui.chat.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ismartcoding.plain.features.chat.download.DownloadTask
import com.ismartcoding.plain.features.chat.models.MessageFile
import com.ismartcoding.plain.lib.extensions.getFilenameExtension
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.extensions.isVideoFast
import com.ismartcoding.plain.ui.media.viewer.previewer.TransformImageViewWithUri
import com.ismartcoding.plain.ui.media.fileIconData
import com.ismartcoding.plain.ui.media.viewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.media.viewer.previewer.TransformItemState

@Composable
fun ChatFileThumbnail(
    fileName: String,
    previewPath: String,
    item: MessageFile,
    itemState: TransformItemState,
    previewerState: MediaPreviewerState,
    isActive: Boolean,
    downloadProgress: Float,
    downloadTask: DownloadTask?,
) {
    val widthPx = with(LocalDensity.current) { 48.dp.toPx() }.toInt()
    Box {
        if (previewPath.isNotEmpty() && (fileName.isImageFast() || fileName.isVideoFast())) {
            TransformImageViewWithUri(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(4.dp)),
                path = previewPath,
                fileName = fileName,
                key = item.id,
                itemState = itemState,
                previewerState = previewerState,
                widthPx = widthPx,
                forceVideoDecoder = fileName.isVideoFast() && !item.isRemoteFile(),
            )
        } else {
            AsyncImage(
                model = fileIconData(fileName.getFilenameExtension()),
                modifier = Modifier.size(48.dp),
                alignment = Alignment.Center,
                contentDescription = fileName,
            )
        }

        if (isActive && downloadTask != null) {
            DownloadProgressOverlay(
                taskId = item.id,
                status = downloadTask.status,
                modifier = Modifier.size(48.dp),
                downloadProgress = downloadProgress,
                size = 32.dp,
                cornerRadius = 4.dp,
            )
        }
    }
}
