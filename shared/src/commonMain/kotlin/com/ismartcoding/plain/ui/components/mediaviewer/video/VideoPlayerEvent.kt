package com.ismartcoding.plain.ui.components.mediaviewer.video

/**
 * Events emitted by [VideoPlayerController], abstracting platform player callbacks.
 */
sealed interface VideoPlayerEvent {
    data class StateChanged(val isPlaying: Boolean, val duration: Long) : VideoPlayerEvent
    data object FirstFrameRendered : VideoPlayerEvent
    data object PositionDiscontinuity : VideoPlayerEvent
}
