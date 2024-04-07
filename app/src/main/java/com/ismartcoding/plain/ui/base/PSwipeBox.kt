package com.ismartcoding.plain.ui.base

import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

enum class SwipeDirection {
    StartToEnd,
    EndToStart,
    Both,
    NONE,
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PSwipeBox(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    state: AnchoredDraggableState<DragAnchors> = rememberAnchoredDraggableState(),
    startContent: @Composable (RowScope.(anchoredDraggableState: AnchoredDraggableState<DragAnchors>) -> Unit)? = null,
    endContent: @Composable (RowScope.(anchoredDraggableState: AnchoredDraggableState<DragAnchors>) -> Unit)? = null,
    content: @Composable BoxScope.(anchoredDraggableState: AnchoredDraggableState<DragAnchors>) -> Unit,
) {
    var startContentWidthPx by remember { mutableFloatStateOf(0f) }
    var endContentWidthPx by remember { mutableFloatStateOf(0f) }
    var bottomHeightPx by remember { mutableIntStateOf(0) }
    val bottomHeightDp = with(LocalDensity.current) {
        bottomHeightPx.toDp()
    }
    val swipeDirection = if (!enabled) {
        SwipeDirection.NONE
    } else if (startContent != null && endContent != null) {
        SwipeDirection.Both
    } else if (startContent != null) {
        SwipeDirection.StartToEnd
    } else if (endContent != null) {
        SwipeDirection.EndToStart
    } else {
        SwipeDirection.NONE
    }
    val draggableAnchors: DraggableAnchors<DragAnchors> = when (swipeDirection) {
        SwipeDirection.StartToEnd -> DraggableAnchors {
            DragAnchors.Start at startContentWidthPx
            DragAnchors.Center at 0f
        }

        SwipeDirection.EndToStart -> DraggableAnchors {
            DragAnchors.Center at 0f
            DragAnchors.End at -endContentWidthPx
        }

        SwipeDirection.Both -> DraggableAnchors {
            DragAnchors.Start at -startContentWidthPx
            DragAnchors.Center at 0f
            DragAnchors.End at endContentWidthPx
        }

        SwipeDirection.NONE -> DraggableAnchors {
            DragAnchors.Center at 0f
        }
    }

    state.updateAnchors(draggableAnchors)

    val offsetRange = when (swipeDirection) {
        SwipeDirection.StartToEnd -> 0f..Float.POSITIVE_INFINITY
        SwipeDirection.EndToStart -> Float.NEGATIVE_INFINITY..0f
        SwipeDirection.Both -> Float.NEGATIVE_INFINITY..Float.POSITIVE_INFINITY
        SwipeDirection.NONE -> 0f..0f
    }
    Box(
        modifier = modifier
            .clipToBounds()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = when (swipeDirection) {
                SwipeDirection.StartToEnd -> Arrangement.Start
                SwipeDirection.EndToStart -> Arrangement.End
                SwipeDirection.Both -> Arrangement.SpaceBetween
                else -> {
                    Arrangement.Center
                }
            }
        ) {
            if (swipeDirection in listOf(
                    SwipeDirection.StartToEnd,
                    SwipeDirection.Both
                ) && startContent != null
            ) {
                Row(
                    modifier = Modifier
                        .height(bottomHeightDp)
                        .onSizeChanged {
                            startContentWidthPx = it.width.toFloat()
                        }
                        .clipToBounds(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    startContent(state)
                }
            }
            if (swipeDirection in listOf(
                    SwipeDirection.EndToStart,
                    SwipeDirection.Both
                ) && endContent != null
            ) {
                Row(
                    modifier = Modifier
                        .height(bottomHeightDp)
                        .onSizeChanged {
                            endContentWidthPx = it.width.toFloat()
                        }
                        .clipToBounds(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    endContent(state)
                }
            }
        }
        Box(modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged {
                bottomHeightPx = it.height
            }
            .offset {
                IntOffset(
                    state
                        .requireOffset()
                        .coerceIn(offsetRange)
                        .roundToInt(), 0
                )
            }
            .anchoredDraggable(
                state,
                Orientation.Horizontal
            )) {
            content(state)
        }
    }
}

enum class DragAnchors {
    Start,
    Center,
    End,
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun rememberAnchoredDraggableState(
    initialValue: DragAnchors = DragAnchors.Center,
    positionalThreshold: (distance: Float) -> Float = { distance -> distance },
    velocityThreshold: Dp = 100.dp,
    animationSpec: TweenSpec<Float> = TweenSpec(durationMillis = 200),
): AnchoredDraggableState<DragAnchors> {
    val density = LocalDensity.current
    return remember {
        AnchoredDraggableState(
            initialValue = initialValue,
            positionalThreshold = positionalThreshold,
            velocityThreshold = { with(density) { velocityThreshold.toPx() } },
            animationSpec = animationSpec
        )
    }
}
