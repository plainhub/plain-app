package com.ismartcoding.plain.ui.base.reorderable

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.launch

internal fun <T> ReorderableLazyCollectionState<T>.onDragStop() {
    val previousDraggingItemInitialOffset = draggingItemLayoutInfo?.offset
    if (draggingItemIndex != null) {
        previousDraggingItemKey = draggingItemKey
        val startOffset = draggingItemOffset
        scope.launch {
            previousDraggingItemOffset.snapTo(startOffset)
            previousDraggingItemOffset.animateTo(
                Offset.Zero,
                spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = Offset.VisibilityThreshold)
            )
            previousDraggingItemKey = null
        }
    }
    draggingItemDraggedDelta = Offset.Zero
    draggingItemKey = null
    draggingItemInitialOffset = previousDraggingItemInitialOffset ?: IntOffset.Zero
    scroller.tryStop()
    oldDraggingItemIndex = null
    predictedDraggingItemOffset = null
}

internal fun <T> ReorderableLazyCollectionState<T>.onDrag(offset: Offset) {
    draggingItemDraggedDelta += offset
    val draggingItem = draggingItemLayoutInfo ?: return
    val dragOffset = draggingItemOffset.reverseAxisIfNecessary()
        .reverseAxisWithLayoutDirectionIfLazyVerticalStaggeredGridRtlFix()
    val startOffset = draggingItem.offset.toOffset() + dragOffset
    val endOffset = startOffset + draggingItem.size.toSize()
    val (contentStartOffset, contentEndOffset) = state.layoutInfo.getScrollAreaOffsets(scrollThresholdPadding)

    val handleOffset = when (state.layoutInfo.reverseLayout ||
            (layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl && orientation == Orientation.Horizontal)) {
        true -> endOffset - draggingItemHandleOffset
        false -> startOffset + draggingItemHandleOffset
    } + IntOffset.fromAxis(orientation, state.layoutInfo.beforeContentPadding).toOffset()

    val distanceFromStart = (handleOffset.getAxis(orientation) - contentStartOffset).coerceAtLeast(0f)
    val distanceFromEnd = (contentEndOffset - handleOffset.getAxis(orientation)).coerceAtLeast(0f)

    val isScrollingStarted = if (distanceFromStart < scrollThreshold) {
        scroller.start(Scroller.Direction.BACKWARD, getScrollSpeedMultiplier(distanceFromStart),
            maxScrollDistanceProvider = {
                (draggingItemLayoutInfo?.let { state.layoutInfo.mainAxisViewportSize - it.offset.toOffset().getAxis(orientation) - 1f }) ?: 0f
            }, onScroll = { moveDraggingItemToEnd(Scroller.Direction.BACKWARD) })
    } else if (distanceFromEnd < scrollThreshold) {
        scroller.start(Scroller.Direction.FORWARD, getScrollSpeedMultiplier(distanceFromEnd),
            maxScrollDistanceProvider = {
                (draggingItemLayoutInfo?.let { it.offset.toOffset().getAxis(orientation) + it.size.getAxis(orientation) - 1f }) ?: 0f
            }, onScroll = { moveDraggingItemToEnd(Scroller.Direction.FORWARD) })
    } else {
        scroller.tryStop(); false
    }

    if (!onMoveStateMutex.tryLock()) return
    if (!scroller.isScrolling && !isScrollingStarted) {
        val draggingItemRect = Rect(startOffset, endOffset)
        val targetItem = findTargetItem(draggingItemRect, items = state.layoutInfo.visibleItemsInfo) { it.index != draggingItem.index }
        if (targetItem != null) { scope.launch { moveItems(draggingItem, targetItem) } }
    }
    onMoveStateMutex.unlock()
}

internal fun <T> ReorderableLazyCollectionState<T>.getScrollSpeedMultiplier(distance: Float): Float {
    return (1 - ((distance + scrollThreshold) / (scrollThreshold * 2)).coerceIn(0f, 1f)) * 10
}
