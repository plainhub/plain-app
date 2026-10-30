package com.ismartcoding.plain.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.features.chat.download.DownloadQueue
import com.ismartcoding.plain.features.chat.download.DownloadTask
import com.ismartcoding.plain.features.chat.models.MessageFile
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.features.chat.models.getPreviewPath
import com.ismartcoding.plain.features.downloads.DownloadStatus
import com.ismartcoding.plain.lib.extensions.formatBytes
import com.ismartcoding.plain.lib.extensions.formatDurationMs
import com.ismartcoding.plain.lib.extensions.isVideoFast
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.ui.media.viewer.previewer.TransformImageViewWithUri
import com.ismartcoding.plain.ui.media.viewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.media.viewer.previewer.rememberTransformItemState
import com.ismartcoding.plain.ui.media.MediaPreviewData
import com.ismartcoding.plain.ui.resources.image as ui_drawable_image
import com.ismartcoding.plain.ui.chat.VChat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun ChatImageItem(
    item: MessageFile,
    items: List<VChat>,
    messageId: String,
    peer: Peer?,
    imageWidthDp: Dp,
    imageWidthPx: Int,
    previewerState: MediaPreviewerState,
    downloadTask: DownloadTask?,
) {
    val source = com.ismartcoding.plain.ui.navigation.LocalContentSource.current
    val downloads = com.ismartcoding.plain.ui.chat.LocalChatDownloads.current
    val itemState = rememberTransformItemState()
    val isRemoteFile = item.isRemoteFile()
    val taskActive = downloadTask?.isActive() == true
    val isFailed = downloadTask?.status == DownloadStatus.FAILED
    val isDownloading = downloadTask?.isDownloading() == true
    val downloadProgress = downloadTask?.let {
        if (it.messageFile.size > 0) it.downloadedSize.toFloat() / it.messageFile.size.toFloat() else 0f
    } ?: 0f
    val previewPath by produceState("", source, item.uri, peer?.id, peer?.address, peer?.ip, peer?.port, peer?.key, peer?.publicKey, peer?.status) {
        value = try {
            if (source is com.ismartcoding.plain.rust.graphql.ContentSource.Remote) { if (item.isRemoteFile()) "" else item.uri }
            else item.getPreviewPath(peer)
        }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { "" }
    }
    val scope = rememberCoroutineScope()

    LaunchedEffect(item.uri) {
        if (isRemoteFile && downloadTask == null && peer != null) {
            downloads.addDownloadTask(item, peer, messageId)
        }
    }

    Box(
        modifier = Modifier.clickable {
            // Allow retry when the download has failed; block navigation only
            // while a download is actively in flight (pending/downloading).
            if (isDownloading) return@clickable
            if (isFailed) {
                downloads.retryDownload(item.id)
                return@clickable
            }
            if (source is com.ismartcoding.plain.rust.graphql.ContentSource.Remote && item.isRemoteFile()) return@clickable
            scope.launch {
                withIO { MediaPreviewData.setDataAsync(itemState, items.reversed(), item, source) }
                previewerState.openTransform(
                    index = MediaPreviewData.items.indexOfFirst { it.id == item.id },
                    itemState = itemState,
                )
            }
        },
    ) {
        if (previewPath.isNotEmpty()) TransformImageViewWithUri(
            modifier = Modifier.size(imageWidthDp).clip(RoundedCornerShape(6.dp)),
            path = previewPath,
            fileName = item.fileName,
            key = item.id,
            itemState = itemState,
            previewerState = previewerState,
            widthPx = imageWidthPx,
            forceVideoDecoder = item.fileName.isVideoFast() && !item.isRemoteFile(),
        ) else Box(Modifier.size(imageWidthDp)) {
            androidx.compose.material3.Icon(
                painter = org.jetbrains.compose.resources.painterResource(com.ismartcoding.plain.ui.resources.Res.drawable.ui_drawable_image),
                contentDescription = item.fileName,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        if (taskActive) {
            DownloadProgressOverlay(
                taskId = item.id,
                status = downloadTask.status,
                modifier = Modifier.size(imageWidthDp),
                downloadProgress = downloadProgress,
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .clip(RoundedCornerShape(bottomEnd = 6.dp))
                .background(Color.Black.copy(alpha = 0.4f)),
        ) {
            val sizeText = if (item.durationMs > 0) item.durationMs.formatDurationMs() else item.size.formatBytes()
            val labelText = if (taskActive) {
                "${(downloadProgress * 100).toInt()}%  $sizeText"
            } else {
                sizeText
            }
            Text(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                text = labelText,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
            )
        }
    }
}
