package com.ismartcoding.plain.ui.base.dragselect

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toIntRect
import com.ismartcoding.plain.ui.Identifiable
import kotlinx.coroutines.isActive

fun Modifier.gridDragSelect(
    items: List<Identifiable>,
    state: DragSelectState,
    enableAutoScroll: Boolean = true,
    autoScrollThreshold: Float? = null,
    enableHaptics: Boolean = true,
    hapticFeedback: HapticFeedback? = null,
): Modifier = composed {
    val itemIndices = remember(items) {
        buildMap {
            items.forEachIndexed { index, item -> if (!containsKey(item.id)) put(item.id, index) }
        }
    }
    val scrollThreshold: Float = autoScrollThreshold ?: GridDragSelectDefaults.autoScrollThreshold
    if (enableAutoScroll) {
        LaunchedEffect(state, state.autoScrollSpeed.floatValue) {
            if (state.autoScrollSpeed.floatValue == 0f) return@LaunchedEffect

            var previousFrame = withFrameNanos { it }
            while (isActive) {
                val frame = withFrameNanos { it }
                val elapsedMs = ((frame - previousFrame) / 1_000_000f).coerceAtMost(50f)
                previousFrame = frame
                state.gridState()?.scrollBy(state.autoScrollSpeed.floatValue * elapsedMs / 10f)
            }
        }
    }

    val haptics: HapticFeedback? =
        if (!enableHaptics) null
        else hapticFeedback ?: GridDragSelectDefaults.hapticsFeedback

    if (!state.selectMode) {
        return@composed this
    }
    // Restart on data changes so a range cannot mix old and new indices.
    pointerInput(state, items, scrollThreshold, haptics) {
        // Helper: find the items-list index of the data item at a touch point.
        // Uses the grid item's key to look up the item in the items list, so it
        // works correctly even when non-data items (e.g. group headers) are in the grid.
        fun findItemIndex(gridState: LazyGridState, position: Offset): Int? {
            val found = gridState.layoutInfo.visibleItemsInfo.find { itemInfo ->
                itemInfo.size.toIntRect().contains(position.round() - itemInfo.offset)
            }
            if (found != null) {
                return itemIndices[found.key]
            }
            // If past the last item, select to the end of the data list
            val lastItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()
                ?.takeIf { it.index == gridState.layoutInfo.totalItemsCount - 1 }
            if (lastItem != null && position.y > lastItem.offset.y) {
                return items.lastIndex.takeIf { it >= 0 }
            }
            return null
        }

        detectDragGestures(
            onDragStart = { offset ->
                state.gridState()?.let { gridState ->
                    findItemIndex(gridState, offset)?.let { startIndex ->
                        haptics?.performHapticFeedback(HapticFeedbackType.LongPress)
                        state.startDrag(items[startIndex].id, startIndex)
                    }
                }
            },
            onDragCancel = state::stopDrag,
            onDragEnd = state::stopDrag,
            onDrag = { change, _ ->
                state.whenDragging { dragState ->
                    val gridState = gridState() ?: return@whenDragging
                    autoScrollSpeed.value = gridState.calculateScrollSpeed(change, scrollThreshold)

                    val itemPosition = findItemIndex(gridState, change.position)
                        ?: return@whenDragging

                    if (itemPosition == dragState.current) {
                        return@whenDragging
                    }

                    val inRangeIds = items.getWithinRangeIds(itemPosition, dragState)
                    val shouldSelect = state.isSelected(dragState.initialId)
                    if (shouldSelect) state.addSelectedIds(inRangeIds)
                    else state.removeSelectedIds(inRangeIds)
                    this.dragState = dragState.copy(current = itemPosition)
                }
            },
        )
    }
}

private fun LazyGridState.calculateScrollSpeed(
    change: PointerInputChange,
    scrollThreshold: Float,
): Float {
    val distanceFromTop: Float = change.position.y
    val distanceFromBottom: Float = layoutInfo.viewportSize.height - distanceFromTop

    return when {
        distanceFromBottom < scrollThreshold -> scrollThreshold - distanceFromBottom
        distanceFromTop < scrollThreshold -> -(scrollThreshold - distanceFromTop)
        else -> 0f
    }
}

private fun List<Identifiable>.getWithinRangeIds(
    itemPosition: Int,
    dragState: DragState,
): List<String> {
    val initial = dragState.initial
    return subList(minOf(initial, itemPosition).coerceAtLeast(0), (maxOf(initial, itemPosition) + 1).coerceAtMost(size)).map { it.id }
}
