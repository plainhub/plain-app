package com.ismartcoding.plain.ui.page.chat.components

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
import com.ismartcoding.plain.lib.extensions.getFilenameExtension
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.extensions.isVideoFast
import com.ismartcoding.plain.chat.download.DownloadQueue
import com.ismartcoding.plain.chat.download.DownloadTask
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.platform.TransformImageView
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.TransformItemState
import com.ismartcoding.plain.platform.getFileIconPath

@Composable
fun ChatFileThumbnail(
    fileName: String,
    previewPath: String,
    item: DMessageFile,
    itemState: TransformItemState,
    previewerState: MediaPreviewerState,
    isActive: Boolean,
    downloadProgress: Float,
    downloadTask: DownloadTask?,
) {
    val widthPx = with(LocalDensity.current) { 48.dp.toPx() }.toInt()
    Box {
        if (fileName.isImageFast() || fileName.isVideoFast()) {
            TransformImageView(
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
                model = getFileIconPath(fileName.getFilenameExtension()),
                modifier = Modifier.size(48.dp),
                alignment = Alignment.Center,
                contentDescription = fileName,
            )
        }

        if (isActive && downloadTask != null) {
            DownloadProgressOverlay(
                modifier = Modifier.size(48.dp),
                downloadProgress = downloadProgress,
                status = downloadTask.status,
                onPause = { DownloadQueue.pauseDownload(item.id) },
                onResume = { DownloadQueue.resumeDownload(item.id) },
                onCancel = { DownloadQueue.removeDownload(item.id) },
                size = 32.dp,
            )
        }
    }
}
