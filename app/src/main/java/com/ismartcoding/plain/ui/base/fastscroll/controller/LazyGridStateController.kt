package com.ismartcoding.plain.ui.base.fastscroll.controller

import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.ui.base.fastscroll.ScrollbarSelectionMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor

@Composable
internal fun rememberLazyGridStateController(
    state: LazyGridState,
    thumbMinLength: Float,
    alwaysShowScrollBar: Boolean,
    selectionMode: ScrollbarSelectionMode,
): LazyGridStateController {
    val coroutineScope = rememberCoroutineScope()

    val thumbMinLengthUpdated = rememberUpdatedState(thumbMinLength)
    val alwaysShowScrollBarUpdated = rememberUpdatedState(alwaysShowScrollBar)
    val selectionModeUpdated = rememberUpdatedState(selectionMode)
    val reverseLayout = remember { derivedStateOf { state.layoutInfo.reverseLayout } }

    val isSelected = remember { mutableStateOf(false) }
    val dragOffset = remember { mutableFloatStateOf(0f) }


    val realFirstVisibleItem = remember {
        derivedStateOf {
            state.layoutInfo.visibleItemsInfo.firstOrNull {
                it.index == state.firstVisibleItemIndex
            }
        }
    }

    // Workaround to know indirectly how many columns/rows are being used (LazyGridState doesn't store it)
    val nElementsMainAxis = remember {
        derivedStateOf {
            var count = 0
            for (item in state.layoutInfo.visibleItemsInfo) {
                val index = item.column
                if (index == -1)
                    break
                if (count == index) {
                    count += 1
                } else {
                    break
                }
            }
            count.coerceAtLeast(1)
        }
    }

    val isStickyHeaderInAction = remember {
        derivedStateOf {
            val realIndex = realFirstVisibleItem.value?.index ?: return@derivedStateOf false
            val firstVisibleIndex = state.layoutInfo.visibleItemsInfo.firstOrNull()?.index
                ?: return@derivedStateOf false
            realIndex != firstVisibleIndex
        }
    }

    fun LazyGridItemInfo.fractionHiddenTop(firstItemOffset: Int): Float {
        return if (size.height == 0) 0f else firstItemOffset / size.width.toFloat()
    }

    fun LazyGridItemInfo.fractionVisibleBottom(viewportEndOffset: Int): Float {
        return if (size.height == 0) 0f else (viewportEndOffset - offset.y).toFloat() / size.height.toFloat()
    }


    val thumbSizeNormalizedReal = remember {
        derivedStateOf {
            state.layoutInfo.let {
                if (it.totalItemsCount == 0)
                    return@let 0f

                val firstItem = realFirstVisibleItem.value ?: return@let 0f
                val firstPartial =
                    firstItem.fractionHiddenTop(state.firstVisibleItemScrollOffset)
                val lastPartial =
                    1f - it.visibleItemsInfo.last().fractionVisibleBottom(it.viewportEndOffset)

                val realSize =
                    ceil(it.visibleItemsInfo.size.toFloat() / nElementsMainAxis.value.toFloat()) - if (isStickyHeaderInAction.value) 1f else 0f
                val realVisibleSize = realSize - firstPartial - lastPartial
                realVisibleSize / ceil(it.totalItemsCount.toFloat() / nElementsMainAxis.value.toFloat())
            }
        }
    }

    val thumbSizeNormalized = remember {
        derivedStateOf {
            thumbSizeNormalizedReal.value.coerceAtLeast(thumbMinLengthUpdated.value)
        }
    }

    fun offsetCorrection(top: Float): Float {
        val topRealMax = (1f - thumbSizeNormalizedReal.value).coerceIn(0f, 1f)
        if (thumbSizeNormalizedReal.value >= thumbMinLengthUpdated.value) {
            return when {
                reverseLayout.value -> topRealMax - top
                else -> top
            }
        }

        val topMax = 1f - thumbMinLengthUpdated.value
        return when {
            reverseLayout.value -> (topRealMax - top) * topMax / topRealMax
            else -> top * topMax / topRealMax
        }
    }

    val thumbOffsetNormalized = remember {
        derivedStateOf {
            state.layoutInfo.let {
                if (it.totalItemsCount == 0 || it.visibleItemsInfo.isEmpty())
                    return@let 0f

                val firstItem = realFirstVisibleItem.value ?: return@let 0f
                val top = firstItem.run {
                    ceil(index.toFloat() / nElementsMainAxis.value.toFloat()) + fractionHiddenTop(state.firstVisibleItemScrollOffset)
                } / ceil(it.totalItemsCount.toFloat() / nElementsMainAxis.value.toFloat())
                offsetCorrection(top)
            }
        }
    }

    val thumbIsInAction = remember {
        derivedStateOf {
            state.isScrollInProgress || isSelected.value || alwaysShowScrollBarUpdated.value
        }
    }

    return remember {
        LazyGridStateController(
            thumbSizeNormalized = thumbSizeNormalized,
            thumbSizeNormalizedReal = thumbSizeNormalizedReal,
            thumbOffsetNormalized = thumbOffsetNormalized,
            thumbIsInAction = thumbIsInAction,
            _isSelected = isSelected,
            dragOffset = dragOffset,
            selectionMode = selectionModeUpdated,
            realFirstVisibleItem = realFirstVisibleItem,
            thumbMinLength = thumbMinLengthUpdated,
            reverseLayout = reverseLayout,
            nElementsMainAxis = nElementsMainAxis,
            state = state,
            coroutineScope = coroutineScope
        )
    }
}

internal class LazyGridStateController(
    override val thumbSizeNormalized: State<Float>,
    override val thumbOffsetNormalized: State<Float>,
    override val thumbIsInAction: State<Boolean>,
    private val _isSelected: MutableState<Boolean>,
    private val dragOffset: MutableFloatState,
    private val selectionMode: State<ScrollbarSelectionMode>,
    private val realFirstVisibleItem: State<LazyGridItemInfo?>,
    private val thumbSizeNormalizedReal: State<Float>,
    private val thumbMinLength: State<Float>,
    private val reverseLayout: State<Boolean>,
    private val nElementsMainAxis: State<Int>,
    private val state: LazyGridState,
    private val coroutineScope: CoroutineScope,
) : StateController<Int> {

    override val isSelected = _isSelected

    override fun indicatorValue(): Int {
        return state.firstVisibleItemIndex
    }

    override fun onDraggableState(deltaPixels: Float, maxLengthPixels: Float) {
        val displace = if (reverseLayout.value) -deltaPixels else deltaPixels // side effect ?
        if (isSelected.value) {
            setScrollOffset(dragOffset.floatValue + displace / maxLengthPixels)
        }
    }

    override fun onDragStarted(offsetPixels: Float, maxLengthPixels: Float) {
        if (maxLengthPixels <= 0f) return
        val newOffset = when {
            reverseLayout.value -> (maxLengthPixels - offsetPixels) / maxLengthPixels
            else -> offsetPixels / maxLengthPixels
        }
        val currentOffset = when {
            reverseLayout.value -> 1f - thumbOffsetNormalized.value - thumbSizeNormalized.value
            else -> thumbOffsetNormalized.value
        }

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

    private fun setScrollOffset(newOffset: Float) {
        setDragOffset(newOffset)
        val totalItemsCount =
            ceil(state.layoutInfo.totalItemsCount.toFloat() / nElementsMainAxis.value.toFloat())
        val exactIndex = offsetCorrectionInverse(totalItemsCount * dragOffset.floatValue)
        val index: Int = floor(exactIndex).toInt() * nElementsMainAxis.value
        val remainder: Float = exactIndex - floor(exactIndex)

        coroutineScope.launch {
            state.scrollToItem(index = index, scrollOffset = 0)
            val offset = realFirstVisibleItem.value
                ?.size
                ?.let {
                    val size = it.height
                    size.toFloat() * remainder
                }
                ?.toInt() ?: 0
            state.scrollToItem(index = index, scrollOffset = offset)
        }
    }

    private fun setDragOffset(value: Float) {
        val maxValue = (1f - thumbSizeNormalized.value).coerceAtLeast(0f)
        dragOffset.floatValue = value.coerceIn(0f, maxValue)
    }

    private fun offsetCorrectionInverse(top: Float): Float {
        if (thumbSizeNormalizedReal.value >= thumbMinLength.value)
            return top
        val topRealMax = 1f - thumbSizeNormalizedReal.value
        val topMax = 1f - thumbMinLength.value
        return top * topRealMax / topMax
    }
}