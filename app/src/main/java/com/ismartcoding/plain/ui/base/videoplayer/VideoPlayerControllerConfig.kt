package com.ismartcoding.plain.ui.base.videoplayer

import android.annotation.SuppressLint
import android.view.View
import androidx.compose.runtime.Immutable
import androidx.core.view.isVisible
import androidx.media3.ui.PlayerView

@Immutable
data class VideoPlayerControllerConfig(
    val showSpeedAndPitchOverlay: Boolean = false,
    val showSubtitleButton: Boolean = false,
    val showCurrentTimeAndTotalTime: Boolean = true,
    val showBufferingProgress: Boolean = true,
    val showForwardIncrementButton: Boolean = true,
    val showBackwardIncrementButton: Boolean = true,
    val showBackTrackButton: Boolean = false,
    val showNextTrackButton: Boolean = false,
    val showRepeatModeButton: Boolean = false,
    val showFullScreenButton: Boolean = true,
    val controllerShowTimeMilliSeconds: Int = 5_000,
    val controllerAutoShow: Boolean = false
)

/**
 * Apply the [VideoPlayerControllerConfig] to the ExoPlayer StyledViewPlayer.
 *
 * @param playerView [PlayerView] to which you want to apply settings.
 * @param onFullScreenStatusChanged Callback that occurs when the full screen status changes.
 */
@SuppressLint("UnsafeOptInUsageError")
internal fun VideoPlayerControllerConfig.applyToExoPlayerView(
    playerView: PlayerView,
    onFullScreenStatusChanged: (Boolean) -> Unit,
) {
    val controllerView = playerView.rootView

    controllerView.findViewById<View>(androidx.media3.ui.R.id.exo_settings).isVisible =
        showSpeedAndPitchOverlay
    playerView.setShowSubtitleButton(showSubtitleButton)
    controllerView.findViewById<View>(androidx.media3.ui.R.id.exo_time).isVisible =
        showCurrentTimeAndTotalTime
    playerView.setShowBuffering(
        if (!showBufferingProgress) PlayerView.SHOW_BUFFERING_NEVER else PlayerView.SHOW_BUFFERING_ALWAYS,
    )
    controllerView.findViewById<View>(androidx.media3.ui.R.id.exo_ffwd_with_amount).isVisible =
        showForwardIncrementButton
    controllerView.findViewById<View>(androidx.media3.ui.R.id.exo_rew_with_amount).isVisible =
        showBackwardIncrementButton
    playerView.setShowNextButton(showNextTrackButton)
    playerView.setShowPreviousButton(showBackTrackButton)
    playerView.setShowFastForwardButton(showForwardIncrementButton)
    playerView.setShowRewindButton(showBackwardIncrementButton)
    playerView.controllerShowTimeoutMs = controllerShowTimeMilliSeconds
    playerView.controllerAutoShow = controllerAutoShow

    @Suppress("DEPRECATION")
    if (showFullScreenButton) {
        playerView.setControllerOnFullScreenModeChangedListener {
            onFullScreenStatusChanged(it)
        }
    } else {
        playerView.setControllerOnFullScreenModeChangedListener(null)
    }
}
