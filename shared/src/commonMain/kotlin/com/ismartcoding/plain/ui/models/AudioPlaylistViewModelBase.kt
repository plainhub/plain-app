package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.MutableState

/**
 * Common interface for the audio playlist view-model surface used by chat pages.
 *
 * The actual [AudioPlaylistViewModel] lives in commonMain and works on both
 * Android (ExoPlayer) and iOS (AVPlayer). Chat UI in commonMain only needs to
 * observe the currently-selected audio path via this interface; the full
 * playlist surface is accessed by casting to [AudioPlaylistViewModel].
 */
interface AudioPlaylistViewModelBase {
    val selectedPath: MutableState<String>
}
