package com.ismartcoding.plain.ui.base.reorderable

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ReorderableListState internal constructor(
    listSize: Int, spacing: Float = 0f,
    private val onMove: () -> Unit, private val onSettle: (fromIndex: Int, toIndex: Int) -> Unit,
    scope: CoroutineScope, private val orientation: Orientation, private val layoutDirection: LayoutDirection,
) {
    internal val itemIntervals = MutableList(listSize) { ItemInterval() }
    internal val itemOffsets = List(listSize) { Animatable(0f) }.toMutableStateList()
    private var draggingItemIndex by mutableStateOf<Int?>(null)
    private var animatingItemIndex by mutableStateOf<Int?>(null)
    internal val isAnyItemDragging by derivedStateOf { draggingItemIndex != null }

    internal val draggableStates = List(listSize) { i ->
        DraggableState {
            if (!isItemDragging(i).value) return@DraggableState
            scope.launch { itemOffsets[i].snapTo(itemOffsets[i].targetValue + it) }
            val originalStart = itemIntervals[i].start
            val originalEnd = itemIntervals[i].end
            val size = itemIntervals[i].size
            val currentStart = itemIntervals[i].start + itemOffsets[i].targetValue
            val currentEnd = currentStart + size
            var moved = false
            itemIntervals.forEachIndexed { j, interval ->
                if (j != i) {
                    val targetOffset = if (currentStart < originalStart && interval.center in currentStart..originalStart) {
                        size.toFloat() + spacing
                    } else if (currentStart > originalStart && interval.center in originalEnd..currentEnd) {
                        -(size.toFloat() + spacing)
                    } else 0f
                    if (itemOffsets[j].targetValue != targetOffset) {
                        scope.launch { itemOffsets[j].animateTo(targetOffset, reorderableAnimationSpec) }
                        moved = true
                    }
                }
            }
            if (moved) onMove()
        }
    }.toMutableStateList()

    internal fun isItemDragging(i: Int): State<Boolean> = derivedStateOf { i == draggingItemIndex }
    internal fun isItemAnimating(i: Int): State<Boolean> = derivedStateOf { i == animatingItemIndex }
    internal fun startDrag(i: Int) { draggingItemIndex = i; animatingItemIndex = i }

    internal suspend fun settle(i: Int, velocity: Float) {
        val originalStart = itemIntervals[i].start
        val originalEnd = itemIntervals[i].end
        val size = itemIntervals[i].size
        val currentStart = itemIntervals[i].start + itemOffsets[i].targetValue
        val currentEnd = currentStart + size
        val targetIndexFunc = if (currentStart < originalStart) { j: Int, interval: ItemInterval ->
            j != i && interval.center in currentStart..<originalStart
        } else if (currentStart > originalStart) { j: Int, interval: ItemInterval ->
            j != i && interval.center in originalEnd..<currentEnd
        } else null
        val targetIndex = targetIndexFunc?.let {
            if (orientation == Orientation.Horizontal && layoutDirection == LayoutDirection.Rtl) {
                if (currentStart < originalStart) itemIntervals.lastIndexOfIndexed(it)
                else if (currentStart > originalStart) itemIntervals.firstIndexOfIndexed(it) else null
            } else {
                if (currentStart < originalStart) itemIntervals.firstIndexOfIndexed(it)
                else if (currentStart > originalStart) itemIntervals.lastIndexOfIndexed(it) else null
            }
        }
        draggingItemIndex = null
        if (targetIndex != null) {
            val offsetToTarget = (itemIntervals[targetIndex].start - itemIntervals[i].start).let {
                if (it > 0) itemIntervals[targetIndex].end - itemIntervals[i].end else it
            }
            itemOffsets[i].animateTo(offsetToTarget, reorderableAnimationSpec, initialVelocity = velocity)
            onSettle(i, targetIndex)
        } else {
            itemOffsets[i].animateTo(0f, reorderableAnimationSpec, initialVelocity = velocity)
        }
        animatingItemIndex = null
    }
}
