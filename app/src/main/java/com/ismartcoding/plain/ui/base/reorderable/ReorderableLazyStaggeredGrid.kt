package com.ismartcoding.plain.ui.base.reorderable

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemInfo
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope

@Composable
fun rememberReorderableLazyStaggeredGridState(
    lazyStaggeredGridState: LazyStaggeredGridState,
    scrollThresholdPadding: PaddingValues = PaddingValues(0.dp),
    scrollThreshold: Dp = ReorderableLazyCollectionDefaults.ScrollThreshold,
    scroller: Scroller = rememberScroller(
        scrollableState = lazyStaggeredGridState,
        pixelAmountProvider = { lazyStaggeredGridState.layoutInfo.mainAxisViewportSize * ScrollAmountMultiplier },
    ),
    onMove: suspend CoroutineScope.(from: LazyStaggeredGridItemInfo, to: LazyStaggeredGridItemInfo) -> Unit,
): ReorderableLazyStaggeredGridState {
    val density = LocalDensity.current
    val scrollThresholdPx = with(density) { scrollThreshold.toPx() }

    val scope = rememberCoroutineScope()
    val onMoveState = rememberUpdatedState(onMove)
    val layoutDirection = LocalLayoutDirection.current
    val absoluteScrollThresholdPadding = AbsolutePixelPadding(
        start = with(density) {
            scrollThresholdPadding.calculateStartPadding(layoutDirection).toPx()
        },
        end = with(density) {
            scrollThresholdPadding.calculateEndPadding(layoutDirection).toPx()
        },
        top = with(density) { scrollThresholdPadding.calculateTopPadding().toPx() },
        bottom = with(density) { scrollThresholdPadding.calculateBottomPadding().toPx() },
    )
    val state = remember(
        scope, lazyStaggeredGridState, scrollThreshold, scrollThresholdPadding, scroller,
    ) {
        ReorderableLazyStaggeredGridState(
            state = lazyStaggeredGridState,
            scope = scope,
            onMoveState = onMoveState,
            scrollThreshold = scrollThresholdPx,
            scrollThresholdPadding = absoluteScrollThresholdPadding,
            scroller = scroller,
            layoutDirection = layoutDirection,
        )
    }
    return state
}

@Stable
class ReorderableLazyStaggeredGridState internal constructor(
    state: LazyStaggeredGridState,
    scope: CoroutineScope,
    onMoveState: State<suspend CoroutineScope.(from: LazyStaggeredGridItemInfo, to: LazyStaggeredGridItemInfo) -> Unit>,

    /**
     * The threshold in pixels for scrolling the grid when dragging an item.
     * If the dragged item is within this threshold of the top or bottom of the grid, the grid will scroll.
     * Must be greater than 0.
     */
    scrollThreshold: Float,
    scrollThresholdPadding: AbsolutePixelPadding,
    scroller: Scroller,
    layoutDirection: LayoutDirection,
) : ReorderableLazyCollectionState<LazyStaggeredGridItemInfo>(
    state.toLazyCollectionState(),
    scope,
    onMoveState,
    scrollThreshold,
    scrollThresholdPadding,
    scroller,
    layoutDirection,
    lazyVerticalStaggeredGridRtlFix = true,
)

/**
 * A composable that allows an item in LazyVerticalStaggeredGrid or LazyHorizontalStaggeredGrid to be reordered by dragging.
 *
 * @param state The return value of [rememberReorderableLazyStaggeredGridState]
 * @param key The key of the item, must be the same as the key passed to [LazyStaggeredGridScope.item](androidx.compose.foundation.lazy.staggeredgrid.item), [LazyStaggeredGridScope.items](androidx.compose.foundation.lazy.staggeredgrid.items) or similar functions in [LazyStaggeredGridScope](androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope)
 * @param enabled Whether or this item is reorderable. If true, the item will not move for other items but may still be draggable. To make an item not draggable, set `enable = false` in [Modifier.draggable] or [Modifier.longPressDraggable] instead.
 * @param animateItemModifier The [Modifier] that will be applied to items that are not being dragged.
 */
@Composable
fun LazyStaggeredGridItemScope.ReorderableItem(
    state: ReorderableLazyStaggeredGridState,
    key: Any,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    animateItemModifier: Modifier = Modifier.animateItem(),
    content: @Composable ReorderableCollectionItemScope.(isDragging: Boolean) -> Unit,
) {
    val dragging by state.isItemDragging(key)
    val offsetModifier = if (dragging) {
        Modifier
            .zIndex(1f)
            .graphicsLayer {
                translationY = state.draggingItemOffset.y
                translationX = state.draggingItemOffset.x
            }
    } else if (key == state.previousDraggingItemKey) {
        Modifier
            .zIndex(1f)
            .graphicsLayer {
                translationY =
                    state.previousDraggingItemOffset.value.y
                translationX =
                    state.previousDraggingItemOffset.value.x
            }
    } else {
        animateItemModifier
    }

    ReorderableCollectionItem(
        state = state,
        key = key,
        modifier = modifier.then(offsetModifier),
        enabled = enabled,
        dragging = dragging,
        content = content,
    )
}
