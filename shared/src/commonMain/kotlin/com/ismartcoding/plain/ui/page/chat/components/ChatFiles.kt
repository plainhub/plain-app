package com.ismartcoding.plain.ui.page.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.download.DownloadQueue
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModelBase
import com.ismartcoding.plain.ui.models.VChat
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal

@Composable
fun ChatFiles(
    items: List<VChat>,
    navController: NavHostController,
    m: VChat,
    peer: DPeer?,
    audioPlaylistVM: AudioPlaylistViewModelBase,
    previewerState: MediaPreviewerState,
) {
    val fileItems = (m.value as DMessageFiles).items
    val currentPlayingPath = audioPlaylistVM.selectedPath
    val downloadProgressMap by DownloadQueue.downloadProgress.collectAsState(mapOf())

    LaunchedEffect(currentPlayingPath.value) {
        if (TempData.audioPlayerVisible.value) {
            TempData.audioPlayerVisible.value = currentPlayingPath.value.isNotEmpty()
        }
    }

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
                audioPlaylistVM = audioPlaylistVM,
                previewerState = previewerState,
                item = item,
                index = index,
                downloadProgressMap = downloadProgressMap,
                onShowAudioPlayer = { TempData.audioPlayerVisible.value = true },
            )
        }
    }

}
