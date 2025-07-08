package com.ismartcoding.plain.ui.page.chat.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.extensions.formatBytes
import com.ismartcoding.lib.extensions.formatDuration
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.chat.DownloadQueue
import com.ismartcoding.plain.db.DMessageImages
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.TransformImageView
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.rememberTransformItemState
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.MediaPreviewData
import com.ismartcoding.plain.ui.models.VChat

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatImages(
    context: Context,
    items: List<VChat>,
    m: VChat,
    peer: DPeer?,
    imageWidthDp: Dp,
    imageWidthPx: Int,
    previewerState: MediaPreviewerState,
    chatViewModel: ChatViewModel,
) {
    val imageItems = (m.value as DMessageImages).items
    val keyboardController = LocalSoftwareKeyboardController.current
    val downloadProgressMap by DownloadQueue.downloadProgress.collectAsState(mapOf())

    FlowRow(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        maxItemsInEachRow = 3,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top),
        content = {
            imageItems.forEach { item ->
                val itemState = rememberTransformItemState()
                val downloadTask = downloadProgressMap[item.id]
                val isRemoteFile = item.isRemoteFile()
                val isDownloading = downloadTask?.isDownloading() == true
                val downloadProgress = downloadTask?.let {
                    if (it.messageFile.size > 0) it.downloadedSize.toFloat() / it.messageFile.size.toFloat() else 0f
                } ?: 0f

                // Auto-start download for remote files
                LaunchedEffect(item.uri) {
                    if (isRemoteFile && downloadTask == null && peer != null) {
                        DownloadQueue.addDownloadTask(item, peer, m.id)
                    }
                }

                Box(
                    modifier =
                        Modifier.clickable {
                            if (isDownloading) {
                                // If currently downloading, do nothing
                                return@clickable
                            }
                            coMain {
                                keyboardController?.hide()
                                withIO { MediaPreviewData.setDataAsync(context, itemState, items.reversed(), item) }
                                previewerState.openTransform(
                                    index = MediaPreviewData.items.indexOfFirst { it.id == item.id },
                                    itemState = itemState,
                                )
                            }
                        },
                ) {
                    TransformImageView(
                        modifier = Modifier
                            .size(imageWidthDp)
                            .clip(RoundedCornerShape(6.dp)),
                        path = item.getPreviewPath(context, peer),
                        key = item.id,
                        itemState = itemState,
                        previewerState = previewerState,
                        widthPx = imageWidthPx
                    )

                    if (isDownloading) {
                        DownloadProgressOverlay(
                            modifier = Modifier
                                .size(imageWidthDp),
                            downloadProgress = downloadProgress,
                            status = downloadTask.status,
                            onPause = { DownloadQueue.pauseDownload(item.id) },
                            onResume = { DownloadQueue.resumeDownload(item.id) },
                            onCancel = { DownloadQueue.removeDownload(item.id) },
                        )
                    }

                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.BottomEnd)
                                .clip(RoundedCornerShape(bottomEnd = 6.dp))
                                .background(Color.Black.copy(alpha = 0.4f)),
                    ) {
                        Text(
                            modifier =
                                Modifier
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                            text =
                                if (item.duration > 0) {
                                    item.duration.formatDuration()
                                } else {
                                    item.size.formatBytes()
                                },
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                        )
                    }
                }
            }
        },
    )
}
