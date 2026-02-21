package com.ismartcoding.plain.ui.page.chat.components

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ismartcoding.lib.extensions.dp2px
import com.ismartcoding.lib.extensions.getFilenameExtension
import com.ismartcoding.lib.extensions.isImageFast
import com.ismartcoding.lib.extensions.isVideoFast
import com.ismartcoding.plain.chat.DownloadQueue
import com.ismartcoding.plain.chat.DownloadTask
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.TransformImageView
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.TransformItemState

@Composable
fun ChatFileThumbnail(
    context: Context,
    fileName: String,
    previewPath: String,
    item: DMessageFile,
    itemState: TransformItemState,
    previewerState: MediaPreviewerState,
    isDownloading: Boolean,
    downloadProgress: Float,
    downloadTask: DownloadTask?,
) {
    Box {
        if (fileName.isImageFast() || fileName.isVideoFast()) {
            TransformImageView(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(4.dp)),
                path = previewPath,
                key = item.id,
                itemState = itemState,
                previewerState = previewerState,
                widthPx = context.dp2px(48),
                forceVideoDecoder = fileName.isVideoFast() && !item.isRemoteFile(),
            )
        } else {
            AsyncImage(
                model = AppHelper.getFileIconPath(fileName.getFilenameExtension()),
                modifier = Modifier.size(48.dp),
                alignment = Alignment.Center,
                contentDescription = fileName,
            )
        }

        if (isDownloading && downloadTask != null) {
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
