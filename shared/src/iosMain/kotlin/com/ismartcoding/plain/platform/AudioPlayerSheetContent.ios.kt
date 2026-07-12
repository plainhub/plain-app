package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModelBase

@Composable
actual fun AudioPlayerSheetContent(
    audioPlaylistVM: AudioPlaylistViewModelBase,
    onDismissRequest: () -> Unit,
) {
    // No-op on iOS: audio player bottom sheet not yet available.
}
