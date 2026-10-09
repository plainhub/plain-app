package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSlider(
    modifier: Modifier = Modifier,
    progress: Float,
    bufferedProgress: Float,
    onProgressChange: (Float) -> Unit,
    colors: PlayerSliderColors = PlayerSliderDefaults.darkColors,
    onValueChangeFinished: ((Float) -> Unit)? = null,
    enabled: Boolean = true,
) {
    val state = rememberPlayerSliderState(progress)
    val latestProgress = rememberUpdatedState(progress)
    val interactions = remember { PlayerSliderInteractionSource { state.cancelDrag(latestProgress.value) } }
    LaunchedEffect(state.seekHoldActive, state.seekRevision) {
        if (state.seekHoldActive) {
            delay(state.seekHoldDurationMs)
            state.expireSeekHold()
        }
    }
    LaunchedEffect(progress, state.seekHoldActive, state.isDragging) {
        state.syncExternalProgress(progress)
    }
    LaunchedEffect(enabled) {
        if (!enabled) state.cancelDrag(progress)
    }
    Slider(
        value = state.displayProgress,
        onValueChange = { value ->
            if (!state.isDragging) state.startDrag(value)
            else state.updateDrag(value - state.displayProgress)
        },
        onValueChangeFinished = {
            if (enabled && state.isDragging) {
                val target = state.endDrag()
                onProgressChange(target)
                onValueChangeFinished?.invoke(target)
            }
        },
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactions,
        thumb = {
            Box(Modifier.size(12.dp).background(colors.thumbColor, CircleShape))
        },
        track = { slider ->
            Canvas(Modifier.fillMaxWidth().height(4.dp)) {
                val rtl = layoutDirection == LayoutDirection.Rtl
                val start = if (rtl) size.width else 0f
                val direction = if (rtl) -1f else 1f
                fun line(fraction: Float, color: androidx.compose.ui.graphics.Color) {
                    drawLine(color, Offset(start, center.y), Offset(start + direction * size.width * fraction.coerceIn(0f, 1f), center.y), size.height, StrokeCap.Round)
                }
                line(1f, colors.trackColor)
                line(bufferedProgress, colors.bufferColor)
                line(slider.value, colors.progressColor)
            }
        },
    )
}
