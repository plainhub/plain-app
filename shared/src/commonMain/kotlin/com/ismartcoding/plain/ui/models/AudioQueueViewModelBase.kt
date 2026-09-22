package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.MutableState

/**
 * Common interface for the audio queue view-model surface used by chat pages.
 *
 * The actual [AudioQueueViewModel] lives in commonMain and works on both
 * Android (ExoPlayer) and iOS (AVPlayer). Chat UI in commonMain only needs to
 * observe the currently-playing audio path via this interface; the full
 * queue surface is accessed by casting to [AudioQueueViewModel].
 */
interface AudioQueueViewModelBase {
    val selectedPath: MutableState<String>
}
