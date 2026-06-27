package com.ismartcoding.plain.ui.base.fastscroll.controller

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.ismartcoding.plain.ui.base.fastscroll.ScrollbarSelectionMode

@Composable
internal fun rememberLazyListStateController(
    state: LazyListState,
    thumbMinLength: Float,
    alwaysShowScrollBar: Boolean,
    selectionMode: ScrollbarSelectionMode
): LazyListStateController {
    val coroutineScope = rememberCoroutineScope()
    val thumbMinLengthUpdated = rememberUpdatedState(thumbMinLength)
    val alwaysShowScrollBarUpdated = rememberUpdatedState(alwaysShowScrollBar)
    val selectionModeUpdated = rememberUpdatedState(selectionMode)
    val reverseLayout = remember { derivedStateOf { state.layoutInfo.reverseLayout } }
    val isSelected = remember { mutableStateOf(false) }
    val dragOffset = remember { mutableFloatStateOf(0f) }

    val realFirstVisibleItem = remember {
        derivedStateOf {
            state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == state.firstVisibleItemIndex }
        }
    }

    val isStickyHeaderInAction = remember {
        derivedStateOf {
            val realIndex = realFirstVisibleItem.value?.index ?: return@derivedStateOf false
            val firstVisibleIndex = state.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: return@derivedStateOf false
            realIndex != firstVisibleIndex
        }
    }

    fun LazyListItemInfo.fractionHiddenTop(firstItemOffset: Int) =
        if (size == 0) 0f else firstItemOffset / size.toFloat()

    fun LazyListItemInfo.fractionVisibleBottom(viewportEndOffset: Int) =
        if (size == 0) 0f else (viewportEndOffset - offset).toFloat() / size.toFloat()

    val thumbSizeNormalizedReal = remember {
        derivedStateOf {
            state.layoutInfo.let {
                if (it.totalItemsCount == 0) return@let 0f
                val firstItem = realFirstVisibleItem.value ?: return@let 0f
                val firstPartial = firstItem.fractionHiddenTop(state.firstVisibleItemScrollOffset)
                val lastPartial = 1f - it.visibleItemsInfo.last().fractionVisibleBottom(it.viewportEndOffset - it.afterContentPadding)
                val realSize = it.visibleItemsInfo.size - if (isStickyHeaderInAction.value) 1 else 0
                val realVisibleSize = realSize.toFloat() - firstPartial - lastPartial
                realVisibleSize / it.totalItemsCount.toFloat()
            }
        }
    }

    val thumbSizeNormalized = remember {
        derivedStateOf { thumbSizeNormalizedReal.value.coerceAtLeast(thumbMinLengthUpdated.value) }
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
                val top = firstItem.run { index.toFloat() + fractionHiddenTop(state.firstVisibleItemScrollOffset) } / it.totalItemsCount.toFloat()
                offsetCorrection(top)
            }
        }
    }

    val thumbIsInAction = remember {
        derivedStateOf { state.isScrollInProgress || isSelected.value || alwaysShowScrollBarUpdated.value }
    }

    return remember {
        LazyListStateController(
            thumbSizeNormalized = thumbSizeNormalized, thumbSizeNormalizedReal = thumbSizeNormalizedReal,
            thumbOffsetNormalized = thumbOffsetNormalized, thumbIsInAction = thumbIsInAction,
            _isSelected = isSelected, dragOffset = dragOffset, selectionMode = selectionModeUpdated,
            realFirstVisibleItem = realFirstVisibleItem, reverseLayout = reverseLayout,
            thumbMinLength = thumbMinLengthUpdated, coroutineScope = coroutineScope, state = state,
        )
    }
}
