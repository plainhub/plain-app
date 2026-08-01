package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModelBase
import com.ismartcoding.plain.ui.page.audio.AudioPlayerPage

/**
 * Full-screen audio player bottom sheet. Renders [AudioPlayerPage] using the
 * supplied view model. Both Android and iOS use [AudioPlaylistViewModel] (its
 * dependencies are in commonMain; playback goes through the platform
 * `AudioPlayer` — ExoPlayer on Android, AVPlayer on iOS), so the cast always
 * succeeds.
 */
@Composable
fun AudioPlayerSheetContent(
    audioPlaylistVM: AudioPlaylistViewModelBase,
    onDismissRequest: () -> Unit,
) {
    val vm = audioPlaylistVM as? AudioPlaylistViewModel ?: return
    AudioPlayerPage(vm, onDismissRequest)
}
