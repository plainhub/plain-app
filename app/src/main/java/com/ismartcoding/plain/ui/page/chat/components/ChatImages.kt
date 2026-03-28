package com.ismartcoding.plain.ui.page.chat.components

import android.content.Context
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
import com.ismartcoding.plain.chat.download.DownloadQueue
import com.ismartcoding.plain.db.DMessageImages
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.models.ChatViewModel
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
    val downloadProgressMap by DownloadQueue.downloadProgress.collectAsState(mapOf())

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        maxItemsInEachRow = 3,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top),
        content = {
            imageItems.forEach { item ->
                ChatImageItem(
                    context = context,
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
