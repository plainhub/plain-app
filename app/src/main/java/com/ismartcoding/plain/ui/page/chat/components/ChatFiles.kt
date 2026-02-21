package com.ismartcoding.plain.ui.page.chat.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.chat.DownloadQueue
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.VChat
import com.ismartcoding.plain.ui.page.audio.AudioPlayerPage
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal

@Composable
fun ChatFiles(
    context: Context,
    items: List<VChat>,
    navController: NavHostController,
    m: VChat,
    peer: DPeer?,
    audioPlaylistVM: AudioPlaylistViewModel,
    previewerState: MediaPreviewerState,
) {
    val fileItems = (m.value as DMessageFiles).items
    val currentPlayingPath = audioPlaylistVM.selectedPath
    var showAudioPlayer by remember { mutableStateOf(false) }
    val downloadProgressMap by DownloadQueue.downloadProgress.collectAsState(mapOf())

    LaunchedEffect(currentPlayingPath.value) {
        if (showAudioPlayer) {
            showAudioPlayer = currentPlayingPath.value.isNotEmpty()
        }
    }

    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.cardBackgroundNormal)
    ) {
        fileItems.forEachIndexed { index, item ->
            ChatFileItem(
                context = context,
                items = items,
                navController = navController,
                m = m,
                peer = peer,
                audioPlaylistVM = audioPlaylistVM,
                previewerState = previewerState,
                item = item,
                index = index,
                downloadProgressMap = downloadProgressMap,
                onShowAudioPlayer = { showAudioPlayer = true },
            )
        }
    }

    if (showAudioPlayer) {
        AudioPlayerPage(
            audioPlaylistVM,
            onDismissRequest = { showAudioPlayer = false },
        )
    }
}
