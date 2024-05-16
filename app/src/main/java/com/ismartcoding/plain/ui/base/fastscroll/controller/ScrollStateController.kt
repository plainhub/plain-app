package com.ismartcoding.plain.ui.base.fastscroll.controller

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.ismartcoding.plain.ui.base.fastscroll.ScrollbarSelectionMode

@Composable
internal fun rememberScrollStateController(
    state: ScrollState,
    visibleLengthDp: Dp,
    thumbMinLength: Float,
    alwaysShowScrollBar: Boolean,
    selectionMode: ScrollbarSelectionMode
): ScrollStateController {
    val coroutineScope = rememberCoroutineScope()

    val visibleLengthDpUpdated = rememberUpdatedState(visibleLengthDp)
    val thumbMinLengthUpdated = rememberUpdatedState(thumbMinLength)
    val alwaysShowScrollBarUpdated = rememberUpdatedState(alwaysShowScrollBar)
    val selectionModeUpdated = rememberUpdatedState(selectionMode)

    val isSelected = remember { mutableStateOf(false) }
    val dragOffset = remember { mutableFloatStateOf(0f) }

    val fullLengthDp = with(LocalDensity.current) {
        remember {
            derivedStateOf {
                state.maxValue.toDp() + visibleLengthDpUpdated.value
            }
        }
    }

    val thumbSizeNormalizedReal = remember {
        derivedStateOf {
            if (fullLengthDp.value == 0.dp) 1f else {
                val normalizedDp = visibleLengthDpUpdated.value / fullLengthDp.value
                normalizedDp.coerceIn(0f, 1f)
            }
        }
    }

    val thumbSizeNormalized = remember {
        derivedStateOf {
            thumbSizeNormalizedReal.value.coerceAtLeast(thumbMinLengthUpdated.value)
        }
    }

    fun offsetCorrection(top: Float): Float {
        val topRealMax = 1f
        val topMax = (1f - thumbSizeNormalized.value).coerceIn(0f, 1f)
        return top * topMax / topRealMax
    }

    val thumbOffsetNormalized = remember {
        derivedStateOf {
            if (state.maxValue == 0) return@derivedStateOf 0f
            val normalized = state.value.toFloat() / state.maxValue.toFloat()
            offsetCorrection(normalized)
        }
    }

    val thumbIsInAction = remember {
        derivedStateOf {
            state.isScrollInProgress || isSelected.value || alwaysShowScrollBarUpdated.value
        }
    }

    return remember {
        ScrollStateController(
            thumbSizeNormalized = thumbSizeNormalized,
            thumbOffsetNormalized = thumbOffsetNormalized,
            thumbIsInAction = thumbIsInAction,
            _isSelected = isSelected,
            dragOffset = dragOffset,
            state = state,
            selectionMode = selectionModeUpdated,
            coroutineScope = coroutineScope
        )
    }
}

internal class ScrollStateController(
    override val thumbSizeNormalized: State<Float>,
    override val thumbOffsetNormalized: State<Float>,
    override val thumbIsInAction: State<Boolean>,
    private val _isSelected: MutableState<Boolean>,
    private val dragOffset: MutableState<Float>,
    private val selectionMode: State<ScrollbarSelectionMode>,
    private val state: ScrollState,
    private val coroutineScope: CoroutineScope,
) : StateController<Float> {
    override val isSelected: State<Boolean> = _isSelected

    override fun onDragStarted(offsetPixels: Float, maxLengthPixels: Float) {
        val newOffset = offsetPixels / maxLengthPixels
        val currentOffset = thumbOffsetNormalized.value

        when (selectionMode.value) {
            ScrollbarSelectionMode.Full -> {
                if (newOffset in currentOffset..(currentOffset + thumbSizeNormalized.value))
                    setDragOffset(currentOffset)
                else
                    setScrollOffset(newOffset)
                _isSelected.value = true
            }

            ScrollbarSelectionMode.Thumb -> {
                if (newOffset in currentOffset..(currentOffset + thumbSizeNormalized.value)) {
                    setDragOffset(currentOffset)
                    _isSelected.value = true
                }
            }

            ScrollbarSelectionMode.Disabled -> Unit
        }
    }

    override fun onDragStopped() {
        _isSelected.value = false
    }

    override fun onDraggableState(deltaPixels: Float, maxLengthPixels: Float) {
        if (isSelected.value) {
            setScrollOffset(dragOffset.value + deltaPixels / maxLengthPixels)
        }
    }

    override fun indicatorValue(): Float {
        return offsetCorrectionInverse(thumbOffsetNormalized.value)
    }

    private fun offsetCorrectionInverse(top: Float): Float {
        val topRealMax = 1f
        val topMax = 1f - thumbSizeNormalized.value
        if (topMax == 0f) return top
        return (top * topRealMax / topMax).coerceAtLeast(0f)
    }

    private fun setScrollOffset(newOffset: Float) {
        setDragOffset(newOffset)
        val exactIndex = offsetCorrectionInverse(state.maxValue * dragOffset.value).toInt()
        coroutineScope.launch {
            state.scrollTo(exactIndex)
        }
    }

    private fun setDragOffset(value: Float) {
        val maxValue = (1f - thumbSizeNormalized.value).coerceAtLeast(0f)
        dragOffset.value = value.coerceIn(0f, maxValue)
    }
}
