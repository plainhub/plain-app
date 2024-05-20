package com.ismartcoding.plain.ui.base.pinchzoomgrid

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.ismartcoding.plain.ui.components.mediaviewer.detectTransformGestures
import kotlinx.coroutines.CoroutineScope

@Composable
fun PinchZoomGridLayout(
    state: PinchZoomGridState,
    modifier: Modifier = Modifier,
    content: @Composable PinchZoomGridScope.() -> Unit,
) {
    val contentScope = remember(state, state.gridState) {
        CurrPinchZoomGridScope(state, state.gridState)
    }

    DisposableEffect(state) {
        onDispose {
            state.cleanup()
        }
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                handlePinchGesture(state)
            }
            .handleOverZooming(state),
    ) {
        val nextCells = state.nextCells
        val scrollPosition = state.gridScrollPosition
        if (nextCells != null && scrollPosition != null) {
            val nextGridState = rememberLazyGridState(
                initialFirstVisibleItemIndex = scrollPosition.firstVisibleItem,
                initialFirstVisibleItemScrollOffset = scrollPosition.firstItemScrollOffset,
            )
            val nextContentScope = remember(nextCells, nextGridState) {
                NextPinchZoomGridScope(
                    state = state,
                    gridState = nextGridState,
                    gridCells = nextCells,
                )
            }

            SideEffect {
                state.nextGridState = nextGridState
            }

            DisposableEffect(state) {
                onDispose {
                    state.nextGridState = null
                }
            }

            // The next grid content during transitions
            content(nextContentScope)
        }

        // The current grid content
        content(contentScope)
    }
}
