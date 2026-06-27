package com.ismartcoding.plain.ui.base.fastscroll.controller

import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.ismartcoding.plain.ui.base.fastscroll.ScrollbarSelectionMode
import kotlin.math.ceil

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

    val nElementsMainAxis = remember {
        derivedStateOf {
            var count = 0
            for (item in state.layoutInfo.visibleItemsInfo) {
                val index = item.column
                if (index == -1) break
                if (count == index) count += 1 else break
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
                if (it.totalItemsCount == 0) return@let 0f
                val firstItem = realFirstVisibleItem.value ?: return@let 0f
                val firstPartial = firstItem.fractionHiddenTop(state.firstVisibleItemScrollOffset)
                val lastPartial = 1f - it.visibleItemsInfo.last().fractionVisibleBottom(it.viewportEndOffset)
                val realSize = ceil(it.visibleItemsInfo.size.toFloat() / nElementsMainAxis.value.toFloat()) - if (isStickyHeaderInAction.value) 1f else 0f
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
            return when { reverseLayout.value -> topRealMax - top; else -> top }
        }
        val topMax = 1f - thumbMinLengthUpdated.value
        return when { reverseLayout.value -> (topRealMax - top) * topMax / topRealMax; else -> top * topMax / topRealMax }
    }

    val thumbOffsetNormalized = remember {
        derivedStateOf {
            state.layoutInfo.let {
                if (it.totalItemsCount == 0 || it.visibleItemsInfo.isEmpty()) return@let 0f
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
