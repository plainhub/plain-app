package com.ismartcoding.plain.ui.base.dragselect

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toIntRect
import com.ismartcoding.plain.data.IData
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

fun Modifier.gridDragSelect(
    items: List<IData>,
    state: DragSelectState,
    enableAutoScroll: Boolean = true,
    autoScrollThreshold: Float? = null,
    enableHaptics: Boolean = true,
    hapticFeedback: HapticFeedback? = null,
): Modifier = composed {
    val scrollThreshold: Float = autoScrollThreshold ?: GridDragSelectDefaults.autoScrollThreshold
    if (enableAutoScroll) {
        LaunchedEffect(state.autoScrollSpeed.floatValue) {
            if (state.autoScrollSpeed.floatValue == 0f) return@LaunchedEffect

            while (isActive) {
                state.gridState()?.scrollBy(state.autoScrollSpeed.floatValue)
                delay(10)
            }
        }
    }

    val haptics: HapticFeedback? =
        if (!enableHaptics) null
        else hapticFeedback ?: GridDragSelectDefaults.hapticsFeedback

    if (!state.selectMode) {
        return@composed this
    }
    pointerInput(Unit) {
        detectDragGestures(
            onDragStart = { offset ->
                state.gridState()?.itemIndexAtPosition(offset)?.let { startIndex ->
                    val item = items.getOrNull(startIndex)
                    if (item != null) {
                        haptics?.performHapticFeedback(HapticFeedbackType.LongPress)
                        state.startDrag(item.id, startIndex)
                    }
                }
            },
            onDragCancel = state::stopDrag,
            onDragEnd = state::stopDrag,
            onDrag = { change, _ ->
                state.whenDragging { dragState ->
                    val gridState = gridState() ?: return@whenDragging
                    autoScrollSpeed.value = gridState.calculateScrollSpeed(change, scrollThreshold)

                    val itemPosition = gridState.getItemPosition(change.position)
                        ?: return@whenDragging

                    if (itemPosition == dragState.current) {
                        return@whenDragging
                    }

                    val inRangeIds = items.getWithinRangeIds(itemPosition, dragState)
                    val shouldSelect = state.isSelected(dragState.initialId)
                    inRangeIds.forEach {
                        if (shouldSelect) {
                            state.addSelected(it)
                        } else {
                            state.removeSelected(it)
                        }
                    }
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

private fun LazyGridState.itemIndexAtPosition(hitPoint: Offset): Int? {
    val found = layoutInfo.visibleItemsInfo.find { itemInfo ->
        itemInfo.size.toIntRect().contains(hitPoint.round() - itemInfo.offset)
    }

    return found?.index
}

fun LazyGridState.getItemPosition(hitPoint: Offset): Int? {
    return itemIndexAtPosition(hitPoint)
        ?: if (isPastLastItem(hitPoint)) layoutInfo.totalItemsCount - 1 else null
}

private fun LazyGridState.isPastLastItem(hitPoint: Offset): Boolean {
    // Get the last item in the list
    val lastItem = layoutInfo.visibleItemsInfo.lastOrNull()
        ?.takeIf { it.index == layoutInfo.totalItemsCount - 1 }
        ?: return false

    // Determine if we have dragged past the last item in the list
    return hitPoint.y > lastItem.offset.y
}

private fun List<IData>.getWithinRangeIds(
    itemPosition: Int,
    dragState: DragState,
): List<String> {
    val initial = dragState.initial
    return filterIndexed { index, _ ->
        index in initial..itemPosition || index in itemPosition..initial
    }.map { it.id }
}

