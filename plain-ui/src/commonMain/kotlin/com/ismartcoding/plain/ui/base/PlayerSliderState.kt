package com.ismartcoding.plain.ui.base

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class PlayerSliderState(
    initialProgress: Float = 0f,
    val seekHoldDurationMs: Long = DEFAULT_SEEK_HOLD_MS,
) {
    var isDragging by mutableStateOf(false)
        private set

    var seekHoldActive by mutableStateOf(false)
        private set

    var dragPosition by mutableFloatStateOf(initialProgress.finiteRatio())
        private set

    var seekRevision by androidx.compose.runtime.mutableIntStateOf(0)
        private set

    fun startDrag(position: Float) {
        isDragging = true
        seekHoldActive = false
        dragPosition = position.finiteRatio()
    }

    fun updateDrag(delta: Float) {
        if (!isDragging) return
        dragPosition = (dragPosition + delta).finiteRatio()
    }

    /** Ends the drag and arms the seek-hold window. Returns the target ratio. */
    fun endDrag(): Float {
        isDragging = false
        seekHoldActive = true
        seekRevision++
        return dragPosition
    }

    fun cancelDrag(currentProgress: Float) {
        isDragging = false
        seekHoldActive = false
        dragPosition = currentProgress.finiteRatio()
    }

    /** Tap-to-seek: arms the seek-hold window and returns the tapped ratio. */
    fun tap(position: Float): Float {
        dragPosition = position.finiteRatio()
        seekHoldActive = true
        seekRevision++
        return dragPosition
    }

    /** Clears the seek-hold window after the timeout fires. */
    fun expireSeekHold() {
        seekHoldActive = false
    }

    /**
     * Called when the parent reports a new external progress value. While
     * dragging or holding, stale updates are ignored so the slider keeps
     * showing the user's intended position.
     */
    fun syncExternalProgress(progress: Float) {
        if (isDragging || seekHoldActive) return
        dragPosition = progress.finiteRatio()
    }

    /** The ratio to render (0..1). Reactive — read during composition. */
    val displayProgress: Float
        get() = dragPosition

    private companion object {
        const val DEFAULT_SEEK_HOLD_MS = 1500L
    }
}

private fun Float.finiteRatio(): Float = if (isFinite()) coerceIn(0f, 1f) else 0f
