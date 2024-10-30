package com.ismartcoding.plain.ui.base.reorderable

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

object ReorderableLazyCollectionDefaults {
    val ScrollThreshold = 48.dp
}

internal const val ScrollAmountMultiplier = 0.05f

internal data class AbsolutePixelPadding(
    val start: Float,
    val end: Float,
    val top: Float,
    val bottom: Float,
) {
    companion object {
        val Zero = AbsolutePixelPadding(0f, 0f, 0f, 0f)

        @Composable
        fun fromPaddingValues(paddingValues: PaddingValues): AbsolutePixelPadding {
            val density = LocalDensity.current
            val layoutDirection = LocalLayoutDirection.current

            return AbsolutePixelPadding(
                start = with(density) {
                    paddingValues.calculateStartPadding(layoutDirection).toPx()
                },
                end = with(density) { paddingValues.calculateEndPadding(layoutDirection).toPx() },
                top = with(density) { paddingValues.calculateTopPadding().toPx() },
                bottom = with(density) { paddingValues.calculateBottomPadding().toPx() },
            )
        }
    }
}

internal interface LazyCollectionItemInfo<out T> {
    val index: Int
    val key: Any
    val offset: IntOffset
    val size: IntSize
    val data: T

    val center: IntOffset
        get() = IntOffset(offset.x + size.width / 2, offset.y + size.height / 2)
}

internal data class CollectionScrollPadding(
    val start: Float,
    val end: Float,
) {
    companion object {
        val Zero = CollectionScrollPadding(0f, 0f)

        fun fromAbsolutePixelPadding(
            orientation: Orientation,
            padding: AbsolutePixelPadding,
            reverseLayout: Boolean,
        ): CollectionScrollPadding {
            return when (orientation) {
                Orientation.Vertical -> CollectionScrollPadding(
                    start = padding.top,
                    end = padding.bottom,
                )

                Orientation.Horizontal -> CollectionScrollPadding(
                    start = padding.start,
                    end = padding.end,
                )
            }.let {
                when (reverseLayout) {
                    true -> CollectionScrollPadding(
                        start = it.end,
                        end = it.start,
                    )

                    false -> it
                }
            }
        }
    }
}

/**
 * The offsets in the main axis from the start of the content to the
 * start and end of the content minus the padding.
 */
internal data class ScrollAreaOffsets(
    val start: Float,
    val end: Float,
)

internal interface LazyCollectionLayoutInfo<out T> {
    val visibleItemsInfo: List<LazyCollectionItemInfo<T>>
    val viewportSize: IntSize
    val orientation: Orientation
    val reverseLayout: Boolean
    val beforeContentPadding: Int

    val mainAxisViewportSize: Int
        get() = when (orientation) {
            Orientation.Vertical -> viewportSize.height
            Orientation.Horizontal -> viewportSize.width
        }

    fun getScrollAreaOffsets(
        padding: AbsolutePixelPadding,
    ) = getScrollAreaOffsets(
        CollectionScrollPadding.fromAbsolutePixelPadding(
            orientation,
            padding,
            reverseLayout,
        )
    )

    fun getScrollAreaOffsets(padding: CollectionScrollPadding): ScrollAreaOffsets {
        val (startPadding, endPadding) = padding
        val contentEndOffset = when (orientation) {
            Orientation.Vertical -> viewportSize.height
            Orientation.Horizontal -> viewportSize.width
        } - endPadding

        return ScrollAreaOffsets(
            start = startPadding,
            end = contentEndOffset,
        )
    }

    /**
     * get items that are fully inside the content area
     */
    fun getItemsInContentArea(padding: AbsolutePixelPadding) = getItemsInContentArea(
        CollectionScrollPadding.fromAbsolutePixelPadding(
            orientation,
            padding,
            reverseLayout,
        )
    )

    /**
     * get items that are fully inside the content area
     */
    fun getItemsInContentArea(padding: CollectionScrollPadding = CollectionScrollPadding.Zero): List<LazyCollectionItemInfo<T>> {
        val (contentStartOffset, contentEndOffset) = getScrollAreaOffsets(
            padding
        )

        return when (orientation) {
            Orientation.Vertical -> {
                visibleItemsInfo.filter { item ->
                    item.offset.y >= contentStartOffset && item.offset.y + item.size.height <= contentEndOffset
                }
            }

            Orientation.Horizontal -> {
                visibleItemsInfo.filter { item ->
                    item.offset.x >= contentStartOffset && item.offset.x + item.size.width <= contentEndOffset
                }
            }
        }
    }
}

internal interface LazyCollectionState<out T> {
    val firstVisibleItemIndex: Int
    val firstVisibleItemScrollOffset: Int
    val layoutInfo: LazyCollectionLayoutInfo<T>

    suspend fun animateScrollBy(
        value: Float,
        animationSpec: AnimationSpec<Float> = spring(),
    ): Float

    suspend fun requestScrollToItem(index: Int, scrollOffset: Int)
}

interface ReorderableLazyCollectionStateInterface {
    val isAnyItemDragging: Boolean
}

// base on https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/foundation/foundation/integration-tests/foundation-demos/src/main/java/androidx/compose/foundation/demos/LazyColumnDragAndDropDemo.kt;drc=edde6e8b9d304264598f962a3b0e5c267e1373bb
// and https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/foundation/foundation/integration-tests/foundation-demos/src/main/java/androidx/compose/foundation/demos/LazyGridDragAndDropDemo.kt;drc=44e4233f7bc0290a1509ef2d448f1309eb63248f
@Stable
open class ReorderableLazyCollectionState<out T> internal constructor(
    private val state: LazyCollectionState<T>,
    private val scope: CoroutineScope,
    private val onMoveState: State<suspend CoroutineScope.(from: T, to: T) -> Unit>,

    /**
     * The threshold in pixels for scrolling the list when dragging an item.
     * If the dragged item is within this threshold of the top or bottom of the list, the list will scroll.
     * Must be greater than 0.
     */
    private val scrollThreshold: Float,
    private val scrollThresholdPadding: AbsolutePixelPadding,
    private val scroller: Scroller,

    private val layoutDirection: LayoutDirection,

    /**
     * Whether this is a LazyVerticalStaggeredGrid
     */
    private val lazyVerticalStaggeredGridRtlFix: Boolean = false,

    /**
     * A function that determines whether the `draggingItem` should be moved with the `item`.
     * Given their bounding rectangles, return `true` if they should be moved.
     * The default implementation is to move if the dragging item's bounding rectangle has crossed the center of the item's bounding rectangle.
     */
    private val shouldItemMove: (draggingItem: Rect, item: Rect) -> Boolean = { draggingItem, item ->
        draggingItem.contains(item.center)
    },
) : ReorderableLazyCollectionStateInterface {
    private val onMoveStateMutex: Mutex = Mutex()

    internal val orientation: Orientation
        get() = state.layoutInfo.orientation

    private var draggingItemKey by mutableStateOf<Any?>(null)
    private val draggingItemIndex: Int?
        get() = draggingItemLayoutInfo?.index

    /**
     * Whether any item is being dragged. This property is observable.
     */
    override val isAnyItemDragging by derivedStateOf {
        draggingItemKey != null
    }

    private var draggingItemDraggedDelta by mutableStateOf(Offset.Zero)
    private var draggingItemInitialOffset by mutableStateOf(IntOffset.Zero)

    // visibleItemsInfo doesn't update immediately after onMove, draggingItemLayoutInfo.item may be outdated for a short time.
    // not a clean solution, but it works.
    private var oldDraggingItemIndex by mutableStateOf<Int?>(null)
    private var predictedDraggingItemOffset by mutableStateOf<IntOffset?>(null)

    private val draggingItemLayoutInfo: LazyCollectionItemInfo<T>?
        get() = draggingItemKey?.let { draggingItemKey ->
            state.layoutInfo.visibleItemsInfo.firstOrNull { it.key == draggingItemKey }
        }
    internal val draggingItemOffset: Offset
        get() = (draggingItemLayoutInfo?.let {
            val offset =
                if (it.index != oldDraggingItemIndex || oldDraggingItemIndex == null) {
                    oldDraggingItemIndex = null
                    predictedDraggingItemOffset = null
                    it.offset
                } else {
                    predictedDraggingItemOffset ?: it.offset
                }

            draggingItemDraggedDelta +
                    (draggingItemInitialOffset.toOffset() - offset.toOffset())
                        .reverseAxisIfNecessary()
                        .reverseAxisWithLayoutDirectionIfLazyVerticalStaggeredGridRtlFix()
        }) ?: Offset.Zero

    // the offset of the handle center from the top left of the dragging item when dragging starts
    private var draggingItemHandleOffset = Offset.Zero

    internal val reorderableKeys = HashSet<Any?>()

    internal var previousDraggingItemKey by mutableStateOf<Any?>(null)
        private set
    internal var previousDraggingItemOffset = Animatable(Offset.Zero, Offset.VectorConverter)
        private set

    private fun Offset.reverseAxisWithReverseLayoutIfNecessary() =
        when (state.layoutInfo.reverseLayout) {
            true -> reverseAxis(orientation)
            false -> this
        }

    private fun Offset.reverseAxisWithLayoutDirectionIfNecessary() = when (orientation) {
        Orientation.Vertical -> this
        Orientation.Horizontal -> reverseAxisWithLayoutDirection()
    }

    private fun Offset.reverseAxisWithLayoutDirection() = when (layoutDirection) {
        LayoutDirection.Ltr -> this
        LayoutDirection.Rtl -> reverseAxis(Orientation.Horizontal)
    }

    private fun Offset.reverseAxisWithLayoutDirectionIfLazyVerticalStaggeredGridRtlFix() =
        when (layoutDirection) {
            LayoutDirection.Ltr -> this
            LayoutDirection.Rtl -> if (lazyVerticalStaggeredGridRtlFix && orientation == Orientation.Vertical)
                reverseAxis(Orientation.Horizontal)
            else this
        }

    private fun Offset.reverseAxisIfNecessary() =
        this.reverseAxisWithReverseLayoutIfNecessary()
            .reverseAxisWithLayoutDirectionIfNecessary()

    private fun Offset.mainAxis() = getAxis(orientation)

    private fun IntOffset.mainAxis() = getAxis(orientation)

    internal suspend fun onDragStart(key: Any, handleOffset: Offset) {
        state.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            item.key == key
        }?.also {
            val mainAxisOffset = it.offset.mainAxis()
            if (mainAxisOffset < 0) {
                // if item is not fully in view, scroll to it
                state.animateScrollBy(mainAxisOffset.toFloat(), spring())
            }

            draggingItemKey = key
            draggingItemInitialOffset = it.offset
            draggingItemHandleOffset = handleOffset
        }
    }

    internal fun onDragStop() {
        val previousDraggingItemInitialOffset = draggingItemLayoutInfo?.offset

        if (draggingItemIndex != null) {
            previousDraggingItemKey = draggingItemKey
            val startOffset = draggingItemOffset
            scope.launch {
                previousDraggingItemOffset.snapTo(startOffset)
                previousDraggingItemOffset.animateTo(
                    Offset.Zero,
                    spring(
                        stiffness = Spring.StiffnessMediumLow,
                        visibilityThreshold = Offset.VisibilityThreshold
                    )
                )
                previousDraggingItemKey = null
            }
        }
        draggingItemDraggedDelta = Offset.Zero
        draggingItemKey = null
        draggingItemInitialOffset = previousDraggingItemInitialOffset ?: IntOffset.Zero
        scroller.tryStop()
        oldDraggingItemIndex = null
        predictedDraggingItemOffset = null
    }

    internal fun onDrag(offset: Offset) {
        draggingItemDraggedDelta += offset

        val draggingItem = draggingItemLayoutInfo ?: return
        // how far the dragging item is from the original position
        val dragOffset = draggingItemOffset.reverseAxisIfNecessary()
            .reverseAxisWithLayoutDirectionIfLazyVerticalStaggeredGridRtlFix()
        val startOffset = draggingItem.offset.toOffset() + dragOffset
        val endOffset = startOffset + draggingItem.size.toSize()
        val (contentStartOffset, contentEndOffset) = state.layoutInfo.getScrollAreaOffsets(
            scrollThresholdPadding
        )

        // the distance from the top or left of the list to the center of the dragging item handle
        val handleOffset =
            when (state.layoutInfo.reverseLayout ||
                    (layoutDirection == LayoutDirection.Rtl &&
                            orientation == Orientation.Horizontal)) {
                true -> endOffset - draggingItemHandleOffset
                false -> startOffset + draggingItemHandleOffset
            } + IntOffset.fromAxis(
                orientation,
                state.layoutInfo.beforeContentPadding
            ).toOffset()

        // check if the handle center is in the scroll threshold
        val distanceFromStart = (handleOffset.getAxis(orientation) - contentStartOffset)
            .coerceAtLeast(0f)
        val distanceFromEnd = (contentEndOffset - handleOffset.getAxis(orientation))
            .coerceAtLeast(0f)

        val isScrollingStarted = if (distanceFromStart < scrollThreshold) {
            scroller.start(
                Scroller.Direction.BACKWARD,
                getScrollSpeedMultiplier(distanceFromStart),
                maxScrollDistanceProvider = {
                    // distance from the start of the dragging item's stationary position to the end of the list
                    (draggingItemLayoutInfo?.let {
                        state.layoutInfo.mainAxisViewportSize -
                                it.offset.toOffset().getAxis(orientation) - 1f
                    }) ?: 0f
                },
                onScroll = {
                    moveDraggingItemToEnd(Scroller.Direction.BACKWARD)
                }
            )
        } else if (distanceFromEnd < scrollThreshold) {
            scroller.start(
                Scroller.Direction.FORWARD,
                getScrollSpeedMultiplier(distanceFromEnd),
                maxScrollDistanceProvider = {
                    // distance from the end of the dragging item's stationary position to the start of the list
                    // the -1f is to prevent the dragging item from being scrolled off and disappearing
                    (draggingItemLayoutInfo?.let {
                        it.offset.toOffset()
                            .getAxis(orientation) + it.size.getAxis(orientation) - 1f
                    }) ?: 0f
                },
                onScroll = {
                    moveDraggingItemToEnd(Scroller.Direction.FORWARD)
                }
            )
        } else {
            scroller.tryStop()
            false
        }

        if (!onMoveStateMutex.tryLock()) return
        if (!scroller.isScrolling && !isScrollingStarted) {
            val draggingItemRect = Rect(startOffset, endOffset)
            // find a target item to move with
            val targetItem = findTargetItem(
                draggingItemRect,
                items = state.layoutInfo.visibleItemsInfo,
            ) {
                it.index != draggingItem.index
            }
            if (targetItem != null) {
                scope.launch {
                    moveItems(draggingItem, targetItem)
                }
            }
        }
        onMoveStateMutex.unlock()
    }

    // keep dragging item in visible area to prevent it from disappearing
    private suspend fun moveDraggingItemToEnd(
        direction: Scroller.Direction,
    ) {
        // wait for the current moveItems to finish
        onMoveStateMutex.lock()

        val draggingItem = draggingItemLayoutInfo
        if (draggingItem == null) {
            onMoveStateMutex.unlock()
            return
        }
        val isDraggingItemAtEnd = when (direction) {
            Scroller.Direction.FORWARD -> draggingItem.index == state.layoutInfo.visibleItemsInfo.lastOrNull()?.index
            Scroller.Direction.BACKWARD -> draggingItem.index == state.firstVisibleItemIndex
        }
        if (isDraggingItemAtEnd) {
            onMoveStateMutex.unlock()
            return
        }
        val dragOffset = draggingItemOffset.reverseAxisIfNecessary()
            .reverseAxisWithLayoutDirectionIfLazyVerticalStaggeredGridRtlFix()
        val startOffset = draggingItem.offset.toOffset() + dragOffset
        val endOffset = startOffset + draggingItem.size.toSize()
        val draggingItemRect = Rect(startOffset, endOffset).maxOutAxis(orientation.opposite)
        val itemsInContentArea = state.layoutInfo.getItemsInContentArea(scrollThresholdPadding)
            // if we can't find an item in the content area but still need to move the dragging item
            // we will need to search outside the content area
            .ifEmpty { state.layoutInfo.visibleItemsInfo }
        val targetItem = findTargetItem(
            draggingItemRect,
            items = itemsInContentArea,
            direction.opposite,
        ) ?: itemsInContentArea.let {
            val targetItemFunc = { item: LazyCollectionItemInfo<T> ->
                item.key in reorderableKeys
            }
            when (direction) {
                Scroller.Direction.FORWARD -> it.findLast(targetItemFunc)
                Scroller.Direction.BACKWARD -> it.find(targetItemFunc)
            }
        }
        if (targetItem == null) {
            onMoveStateMutex.unlock()
            return
        }
        // this solves https://github.com/Calvin-LL/Reorderable/issues/57
        val isTargetDirectionCorrect = when (direction) {
            Scroller.Direction.FORWARD -> targetItem.index > draggingItem.index
            Scroller.Direction.BACKWARD -> targetItem.index < draggingItem.index
        }
        if (!isTargetDirectionCorrect) {
            onMoveStateMutex.unlock()
            return
        }
        val job = scope.launch {
            moveItems(draggingItem, targetItem)
        }
        onMoveStateMutex.unlock()
        job.join()
    }

    private fun Rect.maxOutAxis(orientation: Orientation): Rect {
        return when (orientation) {
            Orientation.Vertical -> copy(
                top = Float.NEGATIVE_INFINITY,
                bottom = Float.POSITIVE_INFINITY,
            )

            Orientation.Horizontal -> copy(
                left = Float.NEGATIVE_INFINITY,
                right = Float.POSITIVE_INFINITY,
            )
        }
    }

    private fun findTargetItem(
        draggingItemRect: Rect,
        items: List<LazyCollectionItemInfo<T>> = state.layoutInfo.getItemsInContentArea(),
        direction: Scroller.Direction = Scroller.Direction.FORWARD,
        additionalPredicate: (LazyCollectionItemInfo<T>) -> Boolean = { true },
    ): LazyCollectionItemInfo<T>? {
        val targetItemFunc = { item: LazyCollectionItemInfo<T> ->
            val targetItemRect = Rect(item.offset.toOffset(), item.size.toSize())

            shouldItemMove(draggingItemRect, targetItemRect)
                    && item.key in reorderableKeys
                    && additionalPredicate(item)
        }
        val targetItem = when (direction) {
            Scroller.Direction.FORWARD -> items.find(targetItemFunc)
            Scroller.Direction.BACKWARD -> items.findLast(targetItemFunc)
        }
        return targetItem
    }

    private val layoutInfoFlow = snapshotFlow { state.layoutInfo }

    companion object {
        const val MoveItemsLayoutInfoUpdateMaxWaitDuration = 1000L
    }

    private suspend fun moveItems(
        draggingItem: LazyCollectionItemInfo<T>,
        targetItem: LazyCollectionItemInfo<T>,
    ) {
        if (draggingItem.index == targetItem.index) return

        if (
            draggingItem.index == state.firstVisibleItemIndex ||
            targetItem.index == state.firstVisibleItemIndex
        ) {
            state.requestScrollToItem(
                state.firstVisibleItemIndex,
                state.firstVisibleItemScrollOffset
            )
        }

        try {
            onMoveStateMutex.withLock {
                oldDraggingItemIndex = draggingItem.index

                scope.(onMoveState.value)(draggingItem.data, targetItem.data)

                predictedDraggingItemOffset = if (targetItem.index > draggingItem.index) {
                    (targetItem.offset + targetItem.size) - draggingItem.size
                } else {
                    targetItem.offset
                }

                withTimeout(MoveItemsLayoutInfoUpdateMaxWaitDuration) {
                    // the first result from layoutInfoFlow is the current layoutInfo
                    // the second result is the updated layoutInfo
                    layoutInfoFlow.take(2).collect()
                }

                oldDraggingItemIndex = null
                predictedDraggingItemOffset = null
            }
        } catch (e: CancellationException) {
            // do nothing
        }
    }

    internal fun isItemDragging(key: Any): State<Boolean> {
        return derivedStateOf {
            key == draggingItemKey
        }
    }

    private fun getScrollSpeedMultiplier(distance: Float): Float {
        // map distance in scrollThreshold..-scrollThreshold to 1..10
        return (1 - ((distance + scrollThreshold) / (scrollThreshold * 2)).coerceIn(
            0f,
            1f
        )) * 10
    }
}

@Stable
interface ReorderableCollectionItemScope {
    /**
     * Make the UI element the draggable handle for the reorderable item.
     *
     * This modifier can only be used on the UI element that is a child of [ReorderableItem].
     *
     * @param enabled Whether or not drag is enabled
     * @param interactionSource [MutableInteractionSource] that will be used to emit [DragInteraction.Start] when this draggable is being dragged.
     * @param onDragStarted The function that is called when the item starts being dragged
     * @param onDragStopped The function that is called when the item stops being dragged
     */
    fun Modifier.draggableHandle(
        enabled: Boolean = true,
        interactionSource: MutableInteractionSource? = null,
        onDragStarted: (startedPosition: Offset) -> Unit = {},
        onDragStopped: () -> Unit = {},
    ): Modifier

    /**
     * Make the UI element the draggable handle for the reorderable item. Drag will start only after a long press.
     *
     * This modifier can only be used on the UI element that is a child of [ReorderableItem].
     *
     * @param enabled Whether or not drag is enabled
     * @param interactionSource [MutableInteractionSource] that will be used to emit [DragInteraction.Start] when this draggable is being dragged.
     * @param onDragStarted The function that is called when the item starts being dragged
     * @param onDragStopped The function that is called when the item stops being dragged
     */
    fun Modifier.longPressDraggableHandle(
        enabled: Boolean = true,
        interactionSource: MutableInteractionSource? = null,
        onDragStarted: (startedPosition: Offset) -> Unit = {},
        onDragStopped: () -> Unit = {},
    ): Modifier
}

internal class ReorderableCollectionItemScopeImpl(
    private val reorderableLazyCollectionState: ReorderableLazyCollectionState<*>,
    private val key: Any,
    private val itemPositionProvider: () -> Offset,
) : ReorderableCollectionItemScope {
    /**
     * Make the UI element the draggable handle for the reorderable item.
     *
     * @param enabled Whether or not drag is enabled
     * @param interactionSource [MutableInteractionSource] that will be used to emit [DragInteraction.Start] when this draggable is being dragged.
     * @param onDragStarted The function that is called when the item starts being dragged
     * @param onDragStopped The function that is called when the item stops being dragged
     */
    override fun Modifier.draggableHandle(
        enabled: Boolean,
        interactionSource: MutableInteractionSource?,
        onDragStarted: (startedPosition: Offset) -> Unit,
        onDragStopped: () -> Unit,
    ) = composed {
        var handleOffset by remember { mutableStateOf(Offset.Zero) }
        var handleSize by remember { mutableStateOf(IntSize.Zero) }

        val coroutineScope = rememberCoroutineScope()

        onGloballyPositioned {
            handleOffset = it.positionInRoot()
            handleSize = it.size
        }.draggable(
            key1 = reorderableLazyCollectionState,
            enabled = enabled && (reorderableLazyCollectionState.isItemDragging(key).value || !reorderableLazyCollectionState.isAnyItemDragging),
            interactionSource = interactionSource,
            onDragStarted = {
                coroutineScope.launch {
                    val handleOffsetRelativeToItem = handleOffset - itemPositionProvider()
                    val handleCenter = Offset(
                        handleOffsetRelativeToItem.x + handleSize.width / 2f,
                        handleOffsetRelativeToItem.y + handleSize.height / 2f
                    )

                    reorderableLazyCollectionState.onDragStart(key, handleCenter)
                }
                onDragStarted(it)
            },
            onDragStopped = {
                reorderableLazyCollectionState.onDragStop()
                onDragStopped()
            },
            onDrag = { change, dragAmount ->
                change.consume()
                reorderableLazyCollectionState.onDrag(dragAmount)
            },
        )
    }

    /**
     * Make the UI element the draggable handle for the reorderable item. Drag will start only after a long press.
     *
     * @param enabled Whether or not drag is enabled
     * @param interactionSource [MutableInteractionSource] that will be used to emit [DragInteraction.Start] when this draggable is being dragged
     * @param onDragStarted The function that is called when the item starts being dragged
     * @param onDragStopped The function that is called when the item stops being dragged
     */
    override fun Modifier.longPressDraggableHandle(
        enabled: Boolean,
        interactionSource: MutableInteractionSource?,
        onDragStarted: (startedPosition: Offset) -> Unit,
        onDragStopped: () -> Unit,
    ) = composed {
        var handleOffset by remember { mutableStateOf(Offset.Zero) }
        var handleSize by remember { mutableStateOf(IntSize.Zero) }

        val coroutineScope = rememberCoroutineScope()

        onGloballyPositioned {
            handleOffset = it.positionInRoot()
            handleSize = it.size
        }.longPressDraggable(
            key1 = reorderableLazyCollectionState,
            enabled = enabled && (reorderableLazyCollectionState.isItemDragging(key).value || !reorderableLazyCollectionState.isAnyItemDragging),
            interactionSource = interactionSource,
            onDragStarted = {
                coroutineScope.launch {
                    val handleOffsetRelativeToItem = handleOffset - itemPositionProvider()
                    val handleCenter = Offset(
                        handleOffsetRelativeToItem.x + handleSize.width / 2f,
                        handleOffsetRelativeToItem.y + handleSize.height / 2f
                    )

                    reorderableLazyCollectionState.onDragStart(key, handleCenter)
                }
                onDragStarted(it)
            },
            onDragStopped = {
                reorderableLazyCollectionState.onDragStop()
                onDragStopped()
            },
            onDrag = { change, dragAmount ->
                change.consume()
                reorderableLazyCollectionState.onDrag(dragAmount)
            },
        )
    }
}

/**
 * A composable that allows items to be reordered by dragging.
 *
 * @param state The return value of [rememberReorderableLazyListState], [rememberReorderableLazyGridState], or [rememberReorderableLazyStaggeredGridState]
 * @param key The key of the item, must be the same as the key passed to the parent composable
 * @param enabled Whether or this item is reorderable. If true, the item will not move for other items but may still be draggable. To make an item not draggable, set `enable = false` in [Modifier.draggable] or [Modifier.longPressDraggable] instead.
 * @param dragging Whether or not this item is currently being dragged
 */
@Composable
internal fun ReorderableCollectionItem(
    state: ReorderableLazyCollectionState<*>,
    key: Any,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    dragging: Boolean,
    content: @Composable ReorderableCollectionItemScope.(isDragging: Boolean) -> Unit,
) {
    var itemPosition by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier.onGloballyPositioned {
            itemPosition = it.positionInRoot()
        }
    ) {
        val itemScope = remember(state, key) {
            ReorderableCollectionItemScopeImpl(
                reorderableLazyCollectionState = state,
                key = key,
                itemPositionProvider = { itemPosition },
            )
        }
        itemScope.content(dragging)
    }

    LaunchedEffect(state.reorderableKeys, enabled) {
        if (enabled) {
            state.reorderableKeys.add(key)
        } else {
            state.reorderableKeys.remove(key)
        }
    }
}
