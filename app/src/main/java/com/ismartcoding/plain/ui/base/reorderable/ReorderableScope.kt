package com.ismartcoding.plain.ui.base.reorderable

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

interface ReorderableScope {
    fun Modifier.draggableHandle(
        enabled: Boolean = true,
        onDragStarted: suspend CoroutineScope.(startedPosition: Offset) -> Unit = {},
        onDragStopped: suspend CoroutineScope.(velocity: Float) -> Unit = {},
        interactionSource: MutableInteractionSource? = null,
    ): Modifier

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
        enabled: Boolean, onDragStarted: suspend CoroutineScope.(startedPosition: Offset) -> Unit,
        onDragStopped: suspend CoroutineScope.(velocity: Float) -> Unit, interactionSource: MutableInteractionSource?,
    ) = draggable(
        state = state.draggableStates[index], orientation = orientation,
        enabled = enabled && (state.isItemDragging(index).value || !state.isAnyItemDragging),
        interactionSource = interactionSource,
        onDragStarted = { state.startDrag(index); onDragStarted(it) },
        onDragStopped = { velocity -> launch { state.settle(index, velocity) }; onDragStopped(velocity) },
    )

    override fun Modifier.longPressDraggableHandle(
        enabled: Boolean, onDragStarted: (startedPosition: Offset) -> Unit,
        onDragStopped: (velocity: Float) -> Unit, interactionSource: MutableInteractionSource?,
    ) = composed {
        val velocityTracker = remember { VelocityTracker() }
        val coroutineScope = rememberCoroutineScope()
        longPressDraggable(
            key1 = state,
            enabled = enabled && (state.isItemDragging(index).value || !state.isAnyItemDragging),
            interactionSource = interactionSource,
            onDragStarted = { state.startDrag(index); onDragStarted(it) },
            onDragStopped = {
                val velocity = velocityTracker.calculateVelocity()
                velocityTracker.resetTracking()
                val velocityVal = when (orientation) { Orientation.Vertical -> velocity.y; Orientation.Horizontal -> velocity.x }
                coroutineScope.launch { state.settle(index, velocityVal) }
                onDragStopped(velocityVal)
            },
            onDrag = { change, dragAmount ->
                velocityTracker.addPointerInputChange(change)
                state.draggableStates[index].dispatchRawDelta(
                    when (orientation) { Orientation.Vertical -> dragAmount.y; Orientation.Horizontal -> dragAmount.x }
                )
            },
        )
    }
}
