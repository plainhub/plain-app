package com.ismartcoding.plain.ui.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.chat.ChatViewModel
import com.ismartcoding.plain.features.chat.download.DownloadQueue
import com.ismartcoding.plain.features.chat.models.MessageImages
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.ui.media.viewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.chat.VChat

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatImages(
    items: List<VChat>,
    m: VChat,
    peer: Peer?,
    imageWidthDp: Dp,
    imageWidthPx: Int,
    previewerState: MediaPreviewerState,
    chatViewModel: ChatViewModel,
) {
    val downloads = com.ismartcoding.plain.ui.chat.LocalChatDownloads.current
    val imageItems = (m.value as MessageImages).items
    val downloadProgressMap by downloads.downloadProgress.collectAsState(mapOf())

    FlowRow(
        modifier = Modifier
            .fillMaxWidth(),
        maxItemsInEachRow = 3,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top),
        content = {
            imageItems.forEach { item ->
                ChatImageItem(
                    item = item,
                    items = items,
                    messageId = m.id,
                    peer = peer,
                    imageWidthDp = imageWidthDp,
                    imageWidthPx = imageWidthPx,
                    previewerState = previewerState,
                    downloadTask = downloadProgressMap[item.id],
                )
            }
        },
    )
}
