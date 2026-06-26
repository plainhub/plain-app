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
import androidx.compose.runtime.Composable
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

@Composable
fun PlayerSlider(
    modifier: Modifier = Modifier,
    progress: Float,
    bufferedProgress: Float,
    onProgressChange: (Float) -> Unit,
    trackColor: Color = Color.DarkGray.copy(alpha = 0.4f),
    bufferColor: Color = Color.Gray,
    progressColor: Color = Color.White,
    thumbColor: Color = Color.White,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val sliderHeight = 4.dp
    val thumbSize = 12.dp
    var size by remember { mutableStateOf(IntSize.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(progress) }
    val density = LocalDensity.current
    val thumbPx = with(density) { thumbSize.toPx() }

    LaunchedEffect(progress) {
        if (!isDragging) {
            dragPosition = progress
        }
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { size = it.size }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val newProgress = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        dragPosition = newProgress
                        onProgressChange(newProgress)
                        onValueChangeFinished?.invoke()
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragPosition = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        isDragging = false
                        onProgressChange(dragPosition)
                        onValueChangeFinished?.invoke()
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onDrag = { _, dragAmount ->
                        val newPosition = dragPosition + dragAmount.x / size.width.toFloat()
                        dragPosition = newPosition.coerceIn(0f, 1f)
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
                .background(trackColor)
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(bufferedProgress)
                .height(sliderHeight)
                .clip(RoundedCornerShape(sliderHeight / 2))
                .background(bufferColor)
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(if (isDragging) dragPosition else progress)
                .height(sliderHeight)
                .clip(RoundedCornerShape(sliderHeight / 2))
                .background(progressColor)
        )

        Box(
            modifier = Modifier
                .size(thumbSize)
                .offset(
                    x = with(density) {
                        ((if (isDragging) dragPosition else progress) * size.width - thumbPx / 2).toDp()
                    },
                    y = 0.dp
                )
                .align(Alignment.CenterStart)
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}