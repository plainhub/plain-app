package com.ismartcoding.plain.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.features.chat.download.DownloadQueue
import com.ismartcoding.plain.features.chat.models.MessageFiles
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.ui.media.viewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.audio.AudioQueueViewModel
import com.ismartcoding.plain.ui.chat.VChat
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal

@Composable
fun ChatFiles(
    items: List<VChat>,
    navController: NavHostController,
    m: VChat,
    peer: Peer?,
    audioQueueVM: AudioQueueViewModel,
    previewerState: MediaPreviewerState,
) {
    val downloads = com.ismartcoding.plain.ui.chat.LocalChatDownloads.current
    val fileItems = (m.value as MessageFiles).items
    val downloadProgressMap by downloads.downloadProgress.collectAsState(mapOf())

    Column(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.cardBackgroundNormal)
    ) {
        fileItems.forEachIndexed { index, item ->
            ChatFileItem(
                items = items,
                navController = navController,
                m = m,
                peer = peer,
                audioQueueVM = audioQueueVM,
                previewerState = previewerState,
                item = item,
                index = index,
                downloadProgressMap = downloadProgressMap,
                onShowAudioPlayer = { audioQueueVM.audioPlayerVisible.value = true },
            )
        }
    }

}
