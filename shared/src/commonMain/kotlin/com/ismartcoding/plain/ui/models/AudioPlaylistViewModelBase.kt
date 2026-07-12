package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.MutableState

/**
 * Common interface for the audio playlist view-model surface used by chat pages.
 *
 * The actual `AudioPlaylistViewModel` is platform-specific (Android) because it
 * depends on `DPlaylistAudio` and Media3. Chat UI in commonMain only needs to
 * observe the currently-selected audio path.
 */
interface AudioPlaylistViewModelBase {
    val selectedPath: MutableState<String>
}
