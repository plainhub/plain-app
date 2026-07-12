package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModelBase

/**
 * Full-screen audio player bottom sheet. Android delegates to [AudioPlayerPage];
 * iOS is a no-op until the audio player is migrated.
 */
@Composable
expect fun AudioPlayerSheetContent(
    audioPlaylistVM: AudioPlaylistViewModelBase,
    onDismissRequest: () -> Unit,
)
