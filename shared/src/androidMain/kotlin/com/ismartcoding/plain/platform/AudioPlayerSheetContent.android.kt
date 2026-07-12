package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModelBase
import com.ismartcoding.plain.ui.page.audio.AudioPlayerPage

@Composable
actual fun AudioPlayerSheetContent(
    audioPlaylistVM: AudioPlaylistViewModelBase,
    onDismissRequest: () -> Unit,
) {
    AudioPlayerPage(audioPlaylistVM as AudioPlaylistViewModel, onDismissRequest)
}
