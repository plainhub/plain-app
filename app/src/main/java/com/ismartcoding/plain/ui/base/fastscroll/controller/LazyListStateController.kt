package com.ismartcoding.plain.ui.base.fastscroll.controller

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.ui.base.fastscroll.ScrollbarSelectionMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.floor

internal class LazyListStateController(
    override val thumbSizeNormalized: State<Float>,
    override val thumbOffsetNormalized: State<Float>,
    override val thumbIsInAction: State<Boolean>,
    private val _isSelected: MutableState<Boolean>,
    private val dragOffset: MutableFloatState,
    private val thumbSizeNormalizedReal: State<Float>,
    private val realFirstVisibleItem: State<LazyListItemInfo?>,
    private val selectionMode: State<ScrollbarSelectionMode>,
    private val reverseLayout: State<Boolean>,
    private val thumbMinLength: State<Float>,
    private val state: LazyListState,
    private val coroutineScope: CoroutineScope,
) : StateController<Int> {
    override val isSelected: State<Boolean> = _isSelected
    private val firstVisibleItemIndex = derivedStateOf { state.firstVisibleItemIndex }

    override fun indicatorValue() = firstVisibleItemIndex.value

    override fun onDraggableState(deltaPixels: Float, maxLengthPixels: Float) {
        val displace = if (reverseLayout.value) -deltaPixels else deltaPixels
        if (isSelected.value) setScrollOffset(dragOffset.floatValue + displace / maxLengthPixels)
    }

    override fun onDragStarted(offsetPixels: Float, maxLengthPixels: Float) {
        if (maxLengthPixels <= 0f) return
        val newOffset = when { reverseLayout.value -> (maxLengthPixels - offsetPixels) / maxLengthPixels; else -> offsetPixels / maxLengthPixels }
        val currentOffset = when { reverseLayout.value -> 1f - thumbOffsetNormalized.value - thumbSizeNormalized.value; else -> thumbOffsetNormalized.value }
        when (selectionMode.value) {
            ScrollbarSelectionMode.Full -> {
                if (newOffset in currentOffset..(currentOffset + thumbSizeNormalized.value)) setDragOffset(currentOffset) else setScrollOffset(newOffset)
                _isSelected.value = true
            }
            ScrollbarSelectionMode.Thumb -> {
                if (newOffset in currentOffset..(currentOffset + thumbSizeNormalized.value)) { setDragOffset(currentOffset); _isSelected.value = true }
            }
            ScrollbarSelectionMode.Disabled -> Unit
        }
    }

    override fun onDragStopped() { _isSelected.value = false }

    private fun setDragOffset(value: Float) {
        val maxValue = (1f - thumbSizeNormalized.value).coerceAtLeast(0f)
        dragOffset.floatValue = value.coerceIn(0f, maxValue)
    }

    private fun offsetCorrectionInverse(top: Float): Float {
        if (thumbSizeNormalizedReal.value >= thumbMinLength.value) return top
        val topRealMax = 1f - thumbSizeNormalizedReal.value
        val topMax = 1f - thumbMinLength.value
        return top * topRealMax / topMax
    }

    private fun setScrollOffset(newOffset: Float) {
        setDragOffset(newOffset)
        val totalItemsCount = state.layoutInfo.totalItemsCount.toFloat()
        val exactIndex = offsetCorrectionInverse(totalItemsCount * dragOffset.floatValue)
        val index: Int = floor(exactIndex).toInt()
        val remainder: Float = exactIndex - floor(exactIndex)
        coroutineScope.launch {
            try {
                state.scrollToItem(index = index, scrollOffset = 0)
                val offset = realFirstVisibleItem.value?.size?.let { it.toFloat() * remainder } ?: 0f
                state.scrollBy(offset)
            } catch (ex: Throwable) {
                LogCat.e(ex.toString())
                ex.printStackTrace()
            }
        }
    }
}
