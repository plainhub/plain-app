package com.ismartcoding.plain.ui.base.reorderable

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

internal suspend fun <T> ReorderableLazyCollectionState<T>.moveDraggingItemToEnd(
    direction: Scroller.Direction,
) {
    onMoveStateMutex.lock()
    val draggingItem = draggingItemLayoutInfo
    if (draggingItem == null) { onMoveStateMutex.unlock(); return }
    val isDraggingItemAtEnd = when (direction) {
        Scroller.Direction.FORWARD -> draggingItem.index == state.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        Scroller.Direction.BACKWARD -> draggingItem.index == state.firstVisibleItemIndex
    }
    if (isDraggingItemAtEnd) { onMoveStateMutex.unlock(); return }
    val dragOffset = draggingItemOffset.reverseAxisIfNecessary()
        .reverseAxisWithLayoutDirectionIfLazyVerticalStaggeredGridRtlFix()
    val startOffset = draggingItem.offset.toOffset() + dragOffset
    val endOffset = startOffset + draggingItem.size.toSize()
    val draggingItemRect = Rect(startOffset, endOffset).maxOutAxis(orientation.opposite)
    val itemsInContentArea = state.layoutInfo.getItemsInContentArea(scrollThresholdPadding)
        .ifEmpty { state.layoutInfo.visibleItemsInfo }
    val targetItem = findTargetItem(draggingItemRect, items = itemsInContentArea, direction.opposite)
        ?: itemsInContentArea.let {
            val targetItemFunc = { item: LazyCollectionItemInfo<T> -> item.key in reorderableKeys }
            when (direction) {
                Scroller.Direction.FORWARD -> it.findLast(targetItemFunc)
                Scroller.Direction.BACKWARD -> it.find(targetItemFunc)
            }
        }
    if (targetItem == null) { onMoveStateMutex.unlock(); return }
    val isTargetDirectionCorrect = when (direction) {
        Scroller.Direction.FORWARD -> targetItem.index > draggingItem.index
        Scroller.Direction.BACKWARD -> targetItem.index < draggingItem.index
    }
    if (!isTargetDirectionCorrect) { onMoveStateMutex.unlock(); return }
    val job = scope.launch { moveItems(draggingItem, targetItem) }
    onMoveStateMutex.unlock()
    job.join()
}

internal fun Rect.maxOutAxis(orientation: Orientation): Rect {
    return when (orientation) {
        Orientation.Vertical -> copy(top = Float.NEGATIVE_INFINITY, bottom = Float.POSITIVE_INFINITY)
        Orientation.Horizontal -> copy(left = Float.NEGATIVE_INFINITY, right = Float.POSITIVE_INFINITY)
    }
}

internal fun <T> ReorderableLazyCollectionState<T>.findTargetItem(
    draggingItemRect: Rect,
    items: List<LazyCollectionItemInfo<T>> = state.layoutInfo.getItemsInContentArea(),
    direction: Scroller.Direction = Scroller.Direction.FORWARD,
    additionalPredicate: (LazyCollectionItemInfo<T>) -> Boolean = { true },
): LazyCollectionItemInfo<T>? {
    val targetItemFunc = { item: LazyCollectionItemInfo<T> ->
        val targetItemRect = Rect(item.offset.toOffset(), item.size.toSize())
        shouldItemMove(draggingItemRect, targetItemRect) && item.key in reorderableKeys && additionalPredicate(item)
    }
    return when (direction) {
        Scroller.Direction.FORWARD -> items.find(targetItemFunc)
        Scroller.Direction.BACKWARD -> items.findLast(targetItemFunc)
    }
}

internal suspend fun <T> ReorderableLazyCollectionState<T>.moveItems(
    draggingItem: LazyCollectionItemInfo<T>,
    targetItem: LazyCollectionItemInfo<T>,
) {
    if (draggingItem.index == targetItem.index) return
    if (draggingItem.index == state.firstVisibleItemIndex || targetItem.index == state.firstVisibleItemIndex) {
        state.requestScrollToItem(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset)
    }
    try {
        onMoveStateMutex.withLock {
            oldDraggingItemIndex = draggingItem.index
            scope.(onMoveState.value)(draggingItem.data, targetItem.data)
            predictedDraggingItemOffset = if (targetItem.index > draggingItem.index) {
                (targetItem.offset + targetItem.size) - draggingItem.size
            } else { targetItem.offset }
            withTimeout(MoveItemsLayoutInfoUpdateMaxWaitDuration) { layoutInfoFlow.take(2).collect() }
            oldDraggingItemIndex = null
            predictedDraggingItemOffset = null
        }
    } catch (_: CancellationException) {}
}
