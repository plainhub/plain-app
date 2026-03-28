package com.ismartcoding.plain.ui.base.fastscroll.controller

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import com.ismartcoding.plain.ui.base.fastscroll.ScrollbarSelectionMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
