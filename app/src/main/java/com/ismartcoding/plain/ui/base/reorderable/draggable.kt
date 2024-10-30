package com.ismartcoding.plain.ui.base.reorderable

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

internal fun Modifier.draggable(
    key1: Any?,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    onDragStarted: (Offset) -> Unit = { },
    onDragStopped: () -> Unit = { },
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
) = composed {
    val coroutineScope = rememberCoroutineScope()
    var dragInteractionStart by remember { mutableStateOf<DragInteraction.Start?>(null) }
    var dragStarted by remember { mutableStateOf(false) }

    DisposableEffect(key1) {
        onDispose {
            if (dragStarted) {
                dragInteractionStart?.also {
                    coroutineScope.launch {
                        interactionSource?.emit(DragInteraction.Cancel(it))
                    }
                }

                if (dragStarted) {
                    onDragStopped()
                }

                dragStarted = false
            }
        }
    }

    pointerInput(key1, enabled) {
        if (enabled) {
            detectDragGestures(
                onDragStart = {
                    dragStarted = true
                    dragInteractionStart = DragInteraction.Start().also {
                        coroutineScope.launch {
                            interactionSource?.emit(it)
                        }
                    }

                    onDragStarted(it)
                },
                onDragEnd = {
                    dragInteractionStart?.also {
                        coroutineScope.launch {
                            interactionSource?.emit(DragInteraction.Stop(it))
                        }
                    }

                    if (dragStarted) {
                        onDragStopped()
                    }

                    dragStarted = false
                },
                onDragCancel = {
                    dragInteractionStart?.also {
                        coroutineScope.launch {
                            interactionSource?.emit(DragInteraction.Cancel(it))
                        }
                    }

                    if (dragStarted) {
                        onDragStopped()
                    }

                    dragStarted = false
                },
                onDrag = onDrag,
            )
        }
    }
}

internal fun Modifier.longPressDraggable(
    key1: Any?,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    onDragStarted: (Offset) -> Unit = { },
    onDragStopped: () -> Unit = { },
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
) = composed {
    val coroutineScope = rememberCoroutineScope()
    var dragInteractionStart by remember { mutableStateOf<DragInteraction.Start?>(null) }
    var dragStarted by remember { mutableStateOf(false) }

    DisposableEffect(key1) {
        onDispose {
            if (dragStarted) {
                dragInteractionStart?.also {
                    coroutineScope.launch {
                        interactionSource?.emit(DragInteraction.Cancel(it))
                    }
                }

                if (dragStarted) {
                    onDragStopped()
                }

                dragStarted = false
            }
        }
    }

    pointerInput(key1, enabled) {
        if (enabled) {
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    dragStarted = true
                    dragInteractionStart = DragInteraction.Start().also {
                        coroutineScope.launch {
                            interactionSource?.emit(it)
                        }
                    }

                    onDragStarted(it)
                },
                onDragEnd = {
                    dragInteractionStart?.also {
                        coroutineScope.launch {
                            interactionSource?.emit(DragInteraction.Stop(it))
                        }
                    }

                    if (dragStarted) {
                        onDragStopped()
                    }

                    dragStarted = false

                },
                onDragCancel = {
                    dragInteractionStart?.also {
                        coroutineScope.launch {
                            interactionSource?.emit(DragInteraction.Cancel(it))
                        }
                    }

                    if (dragStarted) {
                        onDragStopped()
                    }

                    dragStarted = false

                },
                onDrag = onDrag,
            )
        }
    }
}
