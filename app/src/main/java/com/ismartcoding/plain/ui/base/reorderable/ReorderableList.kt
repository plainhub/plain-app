package com.ismartcoding.plain.ui.base.reorderable

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal data class ItemInterval(val start: Float = 0f, val size: Int = 0) {
    val center: Float
        get() = start + size / 2
    val end: Float
        get() = start + size
}

private inline fun <T> List<T>.firstIndexOfIndexed(predicate: (Int, T) -> Boolean): Int? {
    forEachIndexed { i, e -> if (predicate(i, e)) return i }
    return null
}

private inline fun <T> List<T>.lastIndexOfIndexed(predicate: (Int, T) -> Boolean): Int? {
    for (i in lastIndex downTo 0) {
        if (predicate(i, this[i])) return i
    }
    return null
}

private val animationSpec = spring<Float>(
    stiffness = Spring.StiffnessMediumLow
)

class ReorderableListState internal constructor(
    listSize: Int,
    spacing: Float = 0f,
    private val onMove: () -> Unit,
    private val onSettle: (fromIndex: Int, toIndex: Int) -> Unit,
    scope: CoroutineScope,
    private val orientation: Orientation,
    private val layoutDirection: LayoutDirection,
) {
    internal val itemIntervals = MutableList(listSize) { ItemInterval() }
    internal val itemOffsets = List(listSize) {
        Animatable(0f)
    }.toMutableStateList()
    private var draggingItemIndex by mutableStateOf<Int?>(null)
    private var animatingItemIndex by mutableStateOf<Int?>(null)
    internal val isAnyItemDragging by derivedStateOf {
        draggingItemIndex != null
    }
    internal val draggableStates = List(listSize) { i ->
        DraggableState {
            if (!isItemDragging(i).value) return@DraggableState

            scope.launch {
                itemOffsets[i].snapTo(itemOffsets[i].targetValue + it)
            }

            val originalStart = itemIntervals[i].start
            val originalEnd = itemIntervals[i].end
            val size = itemIntervals[i].size
            val currentStart = itemIntervals[i].start + itemOffsets[i].targetValue
            val currentEnd = currentStart + size

            var moved = false

            itemIntervals.forEachIndexed { j, interval ->
                if (j != i) {
                    val targetOffset =
                        if (currentStart < originalStart && interval.center in currentStart..originalStart) {
                            size.toFloat() + spacing
                        } else if (currentStart > originalStart && interval.center in originalEnd..currentEnd) {
                            -(size.toFloat() + spacing)
                        } else {
                            0f
                        }

                    if (itemOffsets[j].targetValue != targetOffset) {
                        scope.launch {
                            itemOffsets[j].animateTo(targetOffset, animationSpec)
                        }
                        moved = true
                    }
                }
            }

            if (moved) {
                onMove()
            }
        }
    }.toMutableStateList()


    internal fun isItemDragging(i: Int): State<Boolean> {
        return derivedStateOf {
            i == draggingItemIndex
        }
    }

    internal fun isItemAnimating(i: Int): State<Boolean> {
        return derivedStateOf {
            i == animatingItemIndex
        }
    }

    internal fun startDrag(i: Int) {
        draggingItemIndex = i
        animatingItemIndex = i
    }

    internal suspend fun settle(i: Int, velocity: Float) {
        val originalStart = itemIntervals[i].start
        val originalEnd = itemIntervals[i].end
        val size = itemIntervals[i].size
        val currentStart = itemIntervals[i].start + itemOffsets[i].targetValue
        val currentEnd = currentStart + size

        val targetIndexFunc =
            if (currentStart < originalStart) { j: Int, interval: ItemInterval ->
                j != i && interval.center in currentStart..<originalStart
            } else if (currentStart > originalStart) { j: Int, interval: ItemInterval ->
                j != i && interval.center in originalEnd..<currentEnd
            } else null
        val targetIndex = targetIndexFunc?.let {
            if (orientation == Orientation.Horizontal && layoutDirection == LayoutDirection.Rtl) {
                if (currentStart < originalStart) {
                    itemIntervals.lastIndexOfIndexed(it)
                } else if (currentStart > originalStart) {
                    itemIntervals.firstIndexOfIndexed(it)
                } else null
            } else {
                if (currentStart < originalStart) {
                    itemIntervals.firstIndexOfIndexed(it)
                } else if (currentStart > originalStart) {
                    itemIntervals.lastIndexOfIndexed(it)
                } else null
            }
        }

        draggingItemIndex = null

        if (targetIndex != null) {
            val offsetToTarget = (itemIntervals[targetIndex].start - itemIntervals[i].start).let {
                if (it > 0) itemIntervals[targetIndex].end - itemIntervals[i].end else it
            }

            itemOffsets[i].animateTo(offsetToTarget, animationSpec, initialVelocity = velocity)

            onSettle(i, targetIndex)
        } else {
            itemOffsets[i].animateTo(0f, animationSpec, initialVelocity = velocity)
        }

        animatingItemIndex = null
    }
}

interface ReorderableScope {
    /**
     * Make the UI element the draggable handle for the reorderable item.
     *
     * @param enabled Whether or not drag is enabled
     * @param interactionSource [MutableInteractionSource] that will be used to emit [DragInteraction.Start] when this draggable is being dragged
     * @param onDragStarted The function that is called when the item starts being dragged
     * @param onDragStopped The function that is called when the item stops being dragged
     */
    fun Modifier.draggableHandle(
        enabled: Boolean = true,
        onDragStarted: suspend CoroutineScope.(startedPosition: Offset) -> Unit = {},
        onDragStopped: suspend CoroutineScope.(velocity: Float) -> Unit = {},
        interactionSource: MutableInteractionSource? = null,
    ): Modifier

    /**
     * Make the UI element the draggable handle for the reorderable item. Drag will start only after a long press.
     *
     * @param enabled Whether or not drag is enabled
     * @param interactionSource [MutableInteractionSource] that will be used to emit [DragInteraction.Start] when this draggable is being dragged
     * @param onDragStarted The function that is called when the item starts being dragged
     * @param onDragStopped The function that is called when the item stops being dragged
     */
    fun Modifier.longPressDraggableHandle(
        enabled: Boolean = true,
        onDragStarted: (startedPosition: Offset) -> Unit = {},
        onDragStopped: (velocity: Float) -> Unit = {},
        interactionSource: MutableInteractionSource? = null,
    ): Modifier
}

internal class ReorderableScopeImpl(
    private val state: ReorderableListState,
    private val orientation: Orientation,
    private val index: Int,
) : ReorderableScope {

    override fun Modifier.draggableHandle(
        enabled: Boolean,
        onDragStarted: suspend CoroutineScope.(startedPosition: Offset) -> Unit,
        onDragStopped: suspend CoroutineScope.(velocity: Float) -> Unit,
        interactionSource: MutableInteractionSource?,
    ) = draggable(
        state = state.draggableStates[index],
        orientation = orientation,
        enabled = enabled && (state.isItemDragging(index).value || !state.isAnyItemDragging),
        interactionSource = interactionSource,
        onDragStarted = {
            state.startDrag(index)
            onDragStarted(it)
        },
        onDragStopped = { velocity ->
            launch { state.settle(index, velocity) }
            onDragStopped(velocity)
        },
    )

    override fun Modifier.longPressDraggableHandle(
        enabled: Boolean,
        onDragStarted: (startedPosition: Offset) -> Unit,
        onDragStopped: (velocity: Float) -> Unit,
        interactionSource: MutableInteractionSource?,
    ) = composed {
        val velocityTracker = remember { VelocityTracker() }
        val coroutineScope = rememberCoroutineScope()

        longPressDraggable(
            key1 = state,
            enabled = enabled && (state.isItemDragging(index).value || !state.isAnyItemDragging),
            interactionSource = interactionSource,
            onDragStarted = {
                state.startDrag(index)
                onDragStarted(it)
            },
            onDragStopped = {
                val velocity = velocityTracker.calculateVelocity()
                velocityTracker.resetTracking()

                val velocityVal = when (orientation) {
                    Orientation.Vertical -> velocity.y
                    Orientation.Horizontal -> velocity.x
                }
                coroutineScope.launch { state.settle(index, velocityVal) }
                onDragStopped(velocityVal)
            },
            onDrag = { change, dragAmount ->
                velocityTracker.addPointerInputChange(change)

                state.draggableStates[index].dispatchRawDelta(
                    when (orientation) {
                        Orientation.Vertical -> dragAmount.y
                        Orientation.Horizontal -> dragAmount.x
                    }
                )
            },
        )
    }
}

/**
 * A vertically list that can be reordered by dragging and dropping.
 *
 * @param list The list of items to display.
 * @param onSettle The function that is called when the list is reordered. This function is only called when the item is dropped.
 * @param verticalArrangement The vertical arrangement of the layout's children.
 * @param horizontalAlignment The horizontal alignment of the layout's children.
 * @param onMove The function that is called when an item is moved to a new position while dragging.
 */
@Composable
fun <T> ReorderableColumn(
    list: List<T>,
    onSettle: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    onMove: () -> Unit = {},
    content: @Composable ReorderableScope.(index: Int, item: T, isDragging: Boolean) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val spacing = with(density) { verticalArrangement.spacing.toPx() }
    val layoutDirection = LocalLayoutDirection.current
    val reorderableListState = remember(list, spacing) {
        ReorderableListState(
            list.size,
            spacing,
            onMove,
            onSettle,
            coroutineScope,
            Orientation.Vertical,
            layoutDirection
        )
    }

    Column(
        modifier = modifier,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
    ) {
        list.forEachIndexed { i, item ->
            val isDragging by reorderableListState.isItemDragging(i)
            val isAnimating by reorderableListState.isItemAnimating(i)

            Box(
                modifier = Modifier
                    .onGloballyPositioned { cord ->
                        reorderableListState.itemIntervals[i] = ItemInterval(
                            start = cord.positionInParent().y, size = cord.size.height
                        )
                    }
                    .graphicsLayer {
                        translationY = reorderableListState.itemOffsets[i].value
                    }
                    .zIndex(if (isAnimating) 1f else 0f),
            ) {
                ReorderableScopeImpl(
                    state = reorderableListState,
                    orientation = Orientation.Vertical,
                    index = i,
                ).content(i, item, isDragging)
            }
        }
    }
}

/**
 * A horizontally list that can be reordered by dragging and dropping.
 *
 * @param list The list of items to display.
 * @param onSettle The function that is called when the list is reordered.
 * @param horizontalArrangement The horizontal arrangement of the layout's children.
 * @param verticalAlignment The vertical alignment of the layout's children.
 * @param onMove The function that is called when an item is moved.
 */
@Composable
fun <T> ReorderableRow(
    list: List<T>,
    onSettle: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    onMove: () -> Unit = {},
    content: @Composable ReorderableScope.(index: Int, item: T, isDragging: Boolean) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val spacing = with(density) { horizontalArrangement.spacing.toPx() }
    val layoutDirection = LocalLayoutDirection.current
    val reorderableListState = remember(list, spacing) {
        ReorderableListState(
            list.size,
            spacing,
            onMove,
            onSettle,
            coroutineScope,
            Orientation.Horizontal,
            layoutDirection
        )
    }

    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment,
    ) {
        list.forEachIndexed { i, item ->
            val isDragging by reorderableListState.isItemDragging(i)
            val isAnimating by reorderableListState.isItemAnimating(i)

            Box(
                modifier = Modifier
                    .onGloballyPositioned { cord ->
                        reorderableListState.itemIntervals[i] = ItemInterval(
                            start = cord.positionInParent().x, size = cord.size.width
                        )
                    }
                    .graphicsLayer {
                        translationX = reorderableListState.itemOffsets[i].value
                    }
                    .zIndex(if (isAnimating) 1f else 0f),
            ) {
                ReorderableScopeImpl(
                    state = reorderableListState,
                    orientation = Orientation.Horizontal,
                    index = i,
                ).content(i, item, isDragging)
            }
        }
    }
}
