package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Color preset for [PlayerSlider].
 *
 * Use [PlayerSliderDefaults.darkColors] for video-player overlays on dark
 * backgrounds, and [PlayerSliderDefaults.lightColors] for surfaces that
 * follow MaterialTheme (e.g. file lists, cast page, chat inline controls).
 */
@Immutable
data class PlayerSliderColors(
    val trackColor: Color,
    val bufferColor: Color,
    val progressColor: Color,
    val thumbColor: Color,
)

object PlayerSliderDefaults {
    /** Colors suited for dark overlays (video players). Used as the slider default. */
    val darkColors: PlayerSliderColors = PlayerSliderColors(
        trackColor = Color.DarkGray.copy(alpha = 0.4f),
        bufferColor = Color.Gray,
        progressColor = Color.White,
        thumbColor = Color.White,
    )

    /** Colors derived from MaterialTheme for light surfaces. Call inside @Composable. */
    @Composable
    fun lightColors(): PlayerSliderColors = PlayerSliderColors(
        trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
        bufferColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
        progressColor = MaterialTheme.colorScheme.primary,
        thumbColor = MaterialTheme.colorScheme.primary,
    )
}

/**
 * State holder that prevents the slider from snapping back to a stale
 * progress value reported by the parent right after a user-initiated seek.
 *
 * Bug this fixes: the parent (e.g. a DLNA cast position poller) may report
 * `progress = 0` for up to ~1 second after [endDrag] because the renderer
 * hasn't yet processed the seek. Without hold, the slider briefly jumps to
 * 0 before catching up. With hold, the user's drag target is retained
 * until either the parent's progress catches up or [seekHoldDurationMs]
 * elapses (whichever comes first).
 *
 * Held as plain [mutableStateOf] so it is unit-testable without Compose UI.
 */
class PlayerSliderState(
    initialProgress: Float = 0f,
    private val seekHoldDurationMs: Long = DEFAULT_SEEK_HOLD_MS,
) {
    var isDragging by mutableStateOf(false)
        private set

    var seekHoldActive by mutableStateOf(false)
        private set

    var dragPosition by mutableFloatStateOf(initialProgress)
        private set

    fun startDrag(position: Float) {
        isDragging = true
        seekHoldActive = false
        dragPosition = position.coerceIn(0f, 1f)
    }

    fun updateDrag(delta: Float) {
        if (!isDragging) return
        dragPosition = (dragPosition + delta).coerceIn(0f, 1f)
    }

    /** Ends the drag and arms the seek-hold window. Returns the target ratio. */
    fun endDrag(): Float {
        isDragging = false
        seekHoldActive = true
        return dragPosition
    }

    fun cancelDrag(currentProgress: Float) {
        isDragging = false
        seekHoldActive = false
        dragPosition = currentProgress
    }

    /** Tap-to-seek: arms the seek-hold window and returns the tapped ratio. */
    fun tap(position: Float): Float {
        dragPosition = position.coerceIn(0f, 1f)
        seekHoldActive = true
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
        dragPosition = progress
    }

    /** The ratio to render (0..1). Reactive — read during composition. */
    val displayProgress: Float
        get() = dragPosition

    val seekHoldDuration: Long
        get() = seekHoldDurationMs

    private companion object {
        const val DEFAULT_SEEK_HOLD_MS = 1500L
    }
}

@Composable
fun rememberPlayerSliderState(initialProgress: Float = 0f): PlayerSliderState =
    remember { PlayerSliderState(initialProgress) }

@Composable
fun PlayerSlider(
    modifier: Modifier = Modifier,
    progress: Float,
    bufferedProgress: Float,
    onProgressChange: (Float) -> Unit,
    colors: PlayerSliderColors = PlayerSliderDefaults.darkColors,
    onValueChangeFinished: ((Float) -> Unit)? = null,
) {
    val state = rememberPlayerSliderState(progress)
    PlayerSliderImpl(
        modifier = modifier,
        progress = progress,
        bufferedProgress = bufferedProgress,
        onProgressChange = onProgressChange,
        colors = colors,
        onValueChangeFinished = onValueChangeFinished,
        state = state,
    )
}

@Composable
private fun PlayerSliderImpl(
    modifier: Modifier,
    progress: Float,
    bufferedProgress: Float,
    onProgressChange: (Float) -> Unit,
    colors: PlayerSliderColors,
    onValueChangeFinished: ((Float) -> Unit)?,
    state: PlayerSliderState,
) {
    val sliderHeight = 4.dp
    val thumbSize = 12.dp
    var size by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val thumbPx = with(density) { thumbSize.toPx() }

    // Drive the seek-hold timeout. When armed, wait out the duration then
    // release so the slider re-syncs to the parent's actual progress.
    LaunchedEffect(state.seekHoldActive) {
        if (state.seekHoldActive) {
            delay(state.seekHoldDuration)
            state.expireSeekHold()
        }
    }

    // Sync external progress into dragPosition when not dragging or holding.
    LaunchedEffect(progress) {
        state.syncExternalProgress(progress)
    }

    // After the hold window closes, re-sync to the latest external progress
    // so the slider doesn't get stuck on a stale target if the parent never
    // caught up.
    LaunchedEffect(state.seekHoldActive, progress) {
        if (!state.isDragging && !state.seekHoldActive) {
            state.syncExternalProgress(progress)
        }
    }

    val renderProgress = state.displayProgress

    Box(
        modifier = modifier
            .onGloballyPositioned { size = it.size }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val newProgress = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        val target = state.tap(newProgress)
                        onProgressChange(target)
                        onValueChangeFinished?.invoke(target)
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        state.startDrag((offset.x / size.width.toFloat()).coerceIn(0f, 1f))
                    },
                    onDragEnd = {
                        val target = state.endDrag()
                        onProgressChange(target)
                        onValueChangeFinished?.invoke(target)
                    },
                    onDragCancel = {
                        state.cancelDrag(progress)
                    },
                    onDrag = { _, dragAmount ->
                        state.updateDrag(dragAmount.x / size.width.toFloat())
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .align(Alignment.Center)
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(sliderHeight)
                .clip(RoundedCornerShape(sliderHeight / 2))
                .background(colors.trackColor)
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(bufferedProgress)
                .height(sliderHeight)
                .clip(RoundedCornerShape(sliderHeight / 2))
                .background(colors.bufferColor)
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(renderProgress)
                .height(sliderHeight)
                .clip(RoundedCornerShape(sliderHeight / 2))
                .background(colors.progressColor)
        )

        Box(
            modifier = Modifier
                .size(thumbSize)
                .offset(
                    x = with(density) {
                        (renderProgress * size.width - thumbPx / 2).toDp()
                    },
                    y = 0.dp
                )
                .align(Alignment.CenterStart)
                .clip(CircleShape)
                .background(colors.thumbColor)
        )
    }
}
