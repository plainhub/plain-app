package com.ismartcoding.plain.ui.base.pinchzoomgrid

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

@Composable
fun rememberPinchZoomGridState(
    cellsList: List<GridCells>,
    initialCellsIndex: Int,
    gridState: LazyGridState = rememberLazyGridState(),
): PinchZoomGridState {
    val coroutineScope = rememberCoroutineScope()
    return remember(coroutineScope, cellsList, initialCellsIndex, gridState) {
        PinchZoomGridState(
            coroutineScope,
            gridState,
            cellsList,
            initialCellsIndex,
        )
    }
}

@Stable
class PinchZoomGridState(
    private val coroutineScope: CoroutineScope,
    internal val gridState: LazyGridState,
    private val cellsList: List<GridCells>,
    initialCellsIndex: Int,
) {
    var currentCells by mutableStateOf(cellsList[initialCellsIndex])
        private set

    val currentCellsIndex: Int
        get() = cellsList.indexOf(currentCells)

    internal var nextCells by mutableStateOf<GridCells?>(null)

    /**
     * Whether the current grid is zooming.
     */
    var isZooming by mutableStateOf(false)
        private set

    /**
     * The current zoom value.
     */
    var zoom by mutableFloatStateOf(1f)
        private set

    internal var zoomType = ZoomType.ZoomIn
        private set

    internal var zoomCentroid: Offset? = null
        private set

    // Animation value
    internal val progress
        get() = if (zoom < 1f) {
            (1f - zoom) / (1f - ZOOM_OUT_FINAL_VAL)
        } else {
            (zoom - 1f) / (ZOOM_IN_FINAL_VAL - 1f)
        }

    // Used to sync the scroll position between two grids
    internal var nextGridState: LazyGridState? = null
    internal var gridScrollPosition by mutableStateOf<GridScrollPosition?>(null)

    // Item sizes and positions
    internal val itemsBounds = mutableMapOf<Any, Rect>()
    internal val nextItemsBounds = mutableMapOf<Any, Rect>()
    internal var isNextItemsBoundsReady = false
        private set

    internal var isCurrItemsVisible by mutableStateOf(true)
        private set
    internal var forceShowNextItems by mutableStateOf(false)
        private set

    internal val animatingKeys = mutableSetOf<Any>()
    internal var animatingKeysSignal by mutableIntStateOf(0)

    private var animationJob: Job? = null

    internal fun onZoomStart(centroid: Offset, startZoom: Float) {
        animationJob?.cancel()
        isZooming = true
        zoomCentroid = centroid
        syncScrollPosition()
        updateZoomTypeAndNextCells(startZoom)
    }

    internal fun onZoom(zoomChange: Float) {
        val newZoom = (zoom * zoomChange).coerceIn(ZOOM_OUT_FINAL_VAL, ZOOM_IN_FINAL_VAL)
        if (newZoom < 1f) {
            if (zoomType != ZoomType.ZoomOut) {
                updateZoomTypeAndNextCells(newZoom)
                syncScrollPosition()
            }
        } else {
            if (zoomType != ZoomType.ZoomIn) {
                updateZoomTypeAndNextCells(newZoom)
                syncScrollPosition()
            }
        }
        this.zoom = newZoom
    }

    internal fun onZoomStopped(velocity: Velocity) = coroutineScope.launch {
        val next = nextCells
        val job = coroutineContext.job
        animationJob = job
        val maxVelocity = max(abs(velocity.x), abs(velocity.y))
        val animationSpec = if (maxVelocity > 1500f) {
            fastAnimationSpec
        } else if (maxVelocity < 500f) {
            slowAnimationSpec
        } else {
            PZ_DEFAULT_ANIMATION_SPEC
        }

        if (progress > 0.5f && next != null) {
            job.invokeOnCompletion { onZoomAnimationEnd(next) }
            val targetValue = if (zoomType == ZoomType.ZoomIn) {
                ZOOM_IN_FINAL_VAL
            } else {
                ZOOM_OUT_FINAL_VAL
            }
            animate(zoom, targetValue, animationSpec = animationSpec) { value, _ ->
                zoom = value
            }
        } else {
            job.invokeOnCompletion { onZoomAnimationEnd(null) }
            animate(zoom, 1f, animationSpec = animationSpec) { value, _ ->
                zoom = value
            }
        }
    }

    /**
     * Set the current cells.
     */
    fun setCurrentCells(index: Int) {
        val currIndex = cellsList.indexOf(currentCells)
        if (index == currIndex || index < 0 || index >= cellsList.size) {
            return
        }
        currentCells = cellsList[index]
    }

    /**
     * Animate to target cells.
     */
    fun animateToCells(index: Int) {
        val currIndex = cellsList.indexOf(currentCells)
        if (index == currIndex || index < 0 || index >= cellsList.size) {
            return
        }
        val isZoomIn = index > currIndex
        val targetZoom = if (isZoomIn) ZOOM_IN_FINAL_VAL else ZOOM_OUT_FINAL_VAL
        zoomType = if (isZoomIn) ZoomType.ZoomIn else ZoomType.ZoomOut
        val nextCells = cellsList[index]
        this.nextCells = nextCells
        syncScrollPosition()
        animationJob?.cancel()
        animationJob = coroutineScope.launch {
            isZooming = true
            coroutineContext.job.invokeOnCompletion {
                onZoomAnimationEnd(nextCells)
            }
            // Start animation after the new layout is ready
            awaitFrame()
            animate(zoom, targetZoom, animationSpec = PZ_DEFAULT_ANIMATION_SPEC) { value, _ ->
                zoom = value
            }
        }
    }

    private fun syncScrollPosition() {
        nextItemsBounds.clear()
        animatingKeys.clear()
        isNextItemsBoundsReady = false
        animatingKeysSignal = 0
        gridState.run {
            val centroid = zoomCentroid
            if (centroid != null) {
                val centroidItem = layoutInfo.visibleItemsInfo.find {
                    Rect(offset = it.offset.toOffset(), size = it.size.toSize())
                        .contains(centroid)
                }
                if (centroidItem != null) {
                    val offset = if (layoutInfo.orientation == Orientation.Vertical) {
                        -centroidItem.offset.y
                    } else {
                        -centroidItem.offset.x
                    }
                    gridScrollPosition = GridScrollPosition(
                        firstVisibleItem = centroidItem.index,
                        firstItemScrollOffset = offset,
                    )
                    return@run
                }
            }
            gridScrollPosition = GridScrollPosition(
                firstVisibleItem = firstVisibleItemIndex,
                firstItemScrollOffset = firstVisibleItemScrollOffset,
            )
        }
        coroutineScope.launch {
            awaitFrame()

            // After the zoom type is changed, the next grid needs to sync the scroll position,
            // and item bounds need to re-calculate
            val scrollPosition = gridScrollPosition
            val nextGridState = nextGridState
            if (scrollPosition != null && nextGridState != null) {
                nextItemsBounds.clear()
                nextGridState.scrollToItem(
                    index = scrollPosition.firstVisibleItem,
                    scrollOffset = scrollPosition.firstItemScrollOffset,
                )
            }

            // Reset the zoom after the next grid is ready
            zoom = 1f
            isNextItemsBoundsReady = true
        }
    }

    private fun onZoomAnimationEnd(targetCells: GridCells?) {
        swapGrids(targetCells)
        isZooming = false
        zoom = 1f
        zoomCentroid = null
        animatingKeys.clear()
        animatingKeysSignal = 0
        isNextItemsBoundsReady = false
    }

    private fun swapGrids(targetCells: GridCells?) {
        fun onSwapped() {
            isCurrItemsVisible = true
            forceShowNextItems = false
            nextCells = null
            gridScrollPosition = null
        }

        val scrollPosition = gridScrollPosition
        if (targetCells == null || scrollPosition == null) {
            // Don't swap
            onSwapped()
            return
        }
        forceShowNextItems = true
        isCurrItemsVisible = false
        currentCells = targetCells
        coroutineScope.launch {
            awaitFrame() // Won't work if call scrollToItem directly
            gridState.scrollToItem(
                index = scrollPosition.firstVisibleItem,
                scrollOffset = scrollPosition.firstItemScrollOffset,
            )
            onSwapped()
        }
    }

    private fun updateZoomTypeAndNextCells(zoom: Float) {
        val currIndex = cellsList.indexOf(currentCells)
        if (zoom < 1f) {
            zoomType = ZoomType.ZoomOut
            nextCells = if (currIndex > 0) {
                cellsList[currIndex - 1]
            } else {
                null
            }
        } else {
            zoomType = ZoomType.ZoomIn
            nextCells = if (currIndex != cellsList.lastIndex) {
                cellsList[currIndex + 1]
            } else {
                null
            }
        }
    }

    internal fun cleanup() {
        animationJob?.cancel()
        animatingKeys.clear()
        itemsBounds.clear()
        nextItemsBounds.clear()
    }

    companion object {
        private const val ZOOM_IN_FINAL_VAL = 1.5f
        private const val ZOOM_OUT_FINAL_VAL = 0.5f
    }
}

internal enum class ZoomType {
    ZoomIn,
    ZoomOut,
}

internal data class GridScrollPosition(
    val firstVisibleItem: Int,
    val firstItemScrollOffset: Int,
)

private val slowAnimationSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessLow,
)

private val PZ_DEFAULT_ANIMATION_SPEC = spring<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy + 0.15f,
    stiffness = Spring.StiffnessLow + 50f,
)

private val fastAnimationSpec = tween<Float>(durationMillis = 225)