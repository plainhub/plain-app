package com.ismartcoding.plain.ui.base.reorderable

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex

@Stable
open class ReorderableLazyCollectionState<out T> internal constructor(
    internal val state: LazyCollectionState<T>,
    internal val scope: CoroutineScope,
    internal val onMoveState: State<suspend CoroutineScope.(from: @UnsafeVariance T, to: @UnsafeVariance T) -> Unit>,
    internal val scrollThreshold: Float,
    internal val scrollThresholdPadding: AbsolutePixelPadding,
    internal val scroller: Scroller,
    internal val layoutDirection: LayoutDirection,
    internal val lazyVerticalStaggeredGridRtlFix: Boolean = false,
    internal val shouldItemMove: (draggingItem: Rect, item: Rect) -> Boolean = { draggingItem, item ->
        draggingItem.contains(item.center)
    },
) : ReorderableLazyCollectionStateInterface {
    internal val onMoveStateMutex: Mutex = Mutex()
    internal val orientation: Orientation get() = state.layoutInfo.orientation
    internal var draggingItemKey by mutableStateOf<Any?>(null)
    internal val draggingItemIndex: Int? get() = draggingItemLayoutInfo?.index
    override val isAnyItemDragging by derivedStateOf { draggingItemKey != null }
    internal var draggingItemDraggedDelta by mutableStateOf(Offset.Zero)
    internal var draggingItemInitialOffset by mutableStateOf(IntOffset.Zero)
    internal var oldDraggingItemIndex by mutableStateOf<Int?>(null)
    internal var predictedDraggingItemOffset by mutableStateOf<IntOffset?>(null)
    internal var draggingItemHandleOffset = Offset.Zero
    internal val reorderableKeys = HashSet<Any?>()
    internal var previousDraggingItemKey by mutableStateOf<Any?>(null)
    internal var previousDraggingItemOffset = Animatable(Offset.Zero, Offset.VectorConverter)

    internal val draggingItemLayoutInfo: LazyCollectionItemInfo<T>?
        get() = draggingItemKey?.let { key -> state.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } }

    internal val draggingItemOffset: Offset
        get() = (draggingItemLayoutInfo?.let {
            val offset = if (it.index != oldDraggingItemIndex || oldDraggingItemIndex == null) {
                oldDraggingItemIndex = null; predictedDraggingItemOffset = null; it.offset
            } else { predictedDraggingItemOffset ?: it.offset }
            draggingItemDraggedDelta + (draggingItemInitialOffset.toOffset() - offset.toOffset())
                .reverseAxisIfNecessary().reverseAxisWithLayoutDirectionIfLazyVerticalStaggeredGridRtlFix()
        }) ?: Offset.Zero

    internal fun Offset.reverseAxisIfNecessary(): Offset {
        val r1 = if (state.layoutInfo.reverseLayout) reverseAxis(orientation) else this
        return when (orientation) {
            Orientation.Vertical -> r1
            Orientation.Horizontal -> when (layoutDirection) {
                LayoutDirection.Ltr -> r1
                LayoutDirection.Rtl -> r1.reverseAxis(Orientation.Horizontal)
            }
        }
    }

    internal fun Offset.reverseAxisWithLayoutDirectionIfLazyVerticalStaggeredGridRtlFix() =
        when (layoutDirection) {
            LayoutDirection.Ltr -> this
            LayoutDirection.Rtl -> if (lazyVerticalStaggeredGridRtlFix && orientation == Orientation.Vertical)
                reverseAxis(Orientation.Horizontal) else this
        }

    internal suspend fun onDragStart(key: Any, handleOffset: Offset) {
        state.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }?.also {
            val mainAxisOffset = it.offset.getAxis(orientation)
            if (mainAxisOffset < 0) state.animateScrollBy(mainAxisOffset.toFloat(), spring())
            draggingItemKey = key; draggingItemInitialOffset = it.offset; draggingItemHandleOffset = handleOffset
        }
    }

    internal fun isItemDragging(key: Any): State<Boolean> = derivedStateOf { key == draggingItemKey }
    internal val layoutInfoFlow = snapshotFlow { state.layoutInfo }

    companion object { const val MoveItemsLayoutInfoUpdateMaxWaitDuration = 1000L }
}

internal const val MoveItemsLayoutInfoUpdateMaxWaitDuration = 1000L
