package com.ismartcoding.plain.ui.base.pinchzoomgrid

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.toSize
import com.ismartcoding.plain.ui.base.pinchzoomgrid.PinchItemTransitions.Companion.All
import com.ismartcoding.plain.ui.base.pinchzoomgrid.PinchItemTransitions.Companion.Alpha
import com.ismartcoding.plain.ui.base.pinchzoomgrid.PinchItemTransitions.Companion.Scale
import com.ismartcoding.plain.ui.base.pinchzoomgrid.PinchItemTransitions.Companion.Translate

interface PinchZoomGridScope {
    /**
     * The lazy grid state used to pass to [LazyVerticalGrid] or [LazyHorizontalGrid].
     */
    val gridState: LazyGridState

    /**
     * The grid cells used to pass to [LazyVerticalGrid] or [LazyHorizontalGrid].
     */
    val gridCells: GridCells

    /**
     * Mark the item to be animatable. The [key] should be the same as the key passed
     * to [LazyGridScope.item] or [LazyGridScope.items].
     */
    fun Modifier.pinchItem(
        key: Any,
        transitions: PinchItemTransitions = All,
        offscreenTransitions: PinchItemTransitions = Alpha + Translate,
    ): Modifier
}

internal class CurrPinchZoomGridScope(
    private val state: PinchZoomGridState,
    override val gridState: LazyGridState,
) : PinchZoomGridScope {
    override val gridCells: GridCells get() = state.currentCells

    override fun Modifier.pinchItem(
        key: Any,
        transitions: PinchItemTransitions,
        offscreenTransitions: PinchItemTransitions,
    ): Modifier {
        val hasScale = transitions.has(Scale)
        val hasTranslate = transitions.has(Translate)
        val offscreenHashAlpha = offscreenTransitions.has(Alpha)
        val offscreenHasScale = offscreenTransitions.has(Scale)
        val offscreenHashTranslate = offscreenTransitions.has(Translate)
        return this
            .onGloballyPositioned { state.itemsBounds[key] = it.bounds() }
            .onDetach { state.itemsBounds.remove(key) }
            .drawWithContent {
                if (state.isCurrItemsVisible) {
                    drawContent()
                }
            }
            .graphicsLayer {
                if (!state.isCurrItemsVisible || !state.isZooming || state.nextCells == null) {
                    return@graphicsLayer
                }

                val progress = state.progress
                val currBounds = state.itemsBounds[key]
                val nextBounds = state.nextItemsBounds[key]
                if (nextBounds == null) {
                    if (state.isNextItemsBoundsReady) {
                        offscreenItemTransitions(
                            hasAlpha = offscreenHashAlpha,
                            hasScale = offscreenHasScale,
                            hasTranslate = offscreenHashTranslate,
                            progress = progress,
                            currBounds = currBounds,
                        )
                    }
                    return@graphicsLayer
                }

                sharedItemTransitions(
                    hasScale = hasScale,
                    hasTranslate = hasTranslate,
                    progress = progress,
                    nextBounds = nextBounds,
                    currBounds = currBounds,
                )

                if (currBounds != null && (hasScale || hasTranslate)) {
                    if (!state.animatingKeys.contains(key)) {
                        // Notify the next grid renders non-animating items
                        state.animatingKeys.add(key)
                        state.animatingKeysSignal++
                    }
                }
            }
    }

    private fun GraphicsLayerScope.offscreenItemTransitions(
        hasAlpha: Boolean,
        hasScale: Boolean,
        hasTranslate: Boolean,
        progress: Float,
        currBounds: Rect?,
    ) {
        val value = 1f - progress

        if (hasAlpha) {
            alpha = value
        }

        if (hasScale) {
            scaleX = value
            scaleY = value
        }

        val zoomCentroid = state.zoomCentroid
        if (hasTranslate &&
            currBounds != null &&
            zoomCentroid != null
        ) {
            val gridLayoutInfo = state.gridState.layoutInfo
            val isHorizontal = gridLayoutInfo.orientation == Orientation.Horizontal
            val gridWidth = gridLayoutInfo.viewportSize.width
            val gridHeight = gridLayoutInfo.viewportSize.height
            if (isHorizontal) {
                translationX = if (currBounds.right <= zoomCentroid.x) {
                    // Move to left
                    -currBounds.right * progress
                } else {
                    // Move to right
                    (gridWidth - currBounds.left) * progress
                }
            } else {
                translationY = if (currBounds.bottom <= zoomCentroid.y) {
                    // Move to top
                    -currBounds.bottom * progress
                } else {
                    // Move to bottom
                    (gridHeight - currBounds.top) * progress
                }
            }
        }
    }

    private fun GraphicsLayerScope.sharedItemTransitions(
        hasScale: Boolean,
        hasTranslate: Boolean,
        progress: Float,
        nextBounds: Rect,
        currBounds: Rect?,
    ) {
        if (hasScale) {
            transformOrigin = TransformOrigin(0f, 0f)
            val targetScaleX = nextBounds.size.width / size.width
            val targetScaleY = nextBounds.size.height / size.height
            scaleX = 1f + (targetScaleX - 1f) * progress
            scaleY = 1f + (targetScaleY - 1f) * progress
        }

        if (hasTranslate && currBounds != null) {
            val targetTranX = nextBounds.left - currBounds.left
            val targetTranY = nextBounds.top - currBounds.top
            translationX = targetTranX * progress
            translationY = targetTranY * progress
        }
    }
}

internal class NextPinchZoomGridScope(
    private val state: PinchZoomGridState,
    override val gridState: LazyGridState,
    override val gridCells: GridCells,
) : PinchZoomGridScope {
    override fun Modifier.pinchItem(
        key: Any,
        transitions: PinchItemTransitions,
        offscreenTransitions: PinchItemTransitions,
    ): Modifier {
        val offscreenHashAlpha = offscreenTransitions.has(Alpha)
        val offscreenHasScale = offscreenTransitions.has(Scale)
        val offscreenHashTranslate = offscreenTransitions.has(Translate)
        return this
            .onGloballyPositioned { state.nextItemsBounds[key] = it.bounds() }
            .drawWithContent {
                if (state.forceShowNextItems || needToDraw(key)) {
                    drawContent()
                }
            }
            .graphicsLayer {
                if (!state.isZooming) {
                    return@graphicsLayer
                }
                offscreenItemTransitions(
                    hasAlpha = offscreenHashAlpha,
                    hasScale = offscreenHasScale,
                    hasTranslate = offscreenHashTranslate,
                    progress = state.progress,
                    bounds = state.nextItemsBounds[key],
                )
            }
    }

    private fun needToDraw(key: Any): Boolean {
        return state.isZooming &&
                state.animatingKeysSignal > 0 &&
                !state.animatingKeys.contains(key)
    }

    private fun GraphicsLayerScope.offscreenItemTransitions(
        hasAlpha: Boolean,
        hasScale: Boolean,
        hasTranslate: Boolean,
        progress: Float,
        bounds: Rect?,
    ) {
        if (hasAlpha) {
            alpha = progress
        }

        if (hasScale) {
            scaleX = progress
            scaleY = progress
        }

        val zoomCentroid = state.zoomCentroid
        if (hasTranslate &&
            bounds != null &&
            zoomCentroid != null
        ) {
            val gridLayoutInfo = state.gridState.layoutInfo
            val isHorizontal = gridLayoutInfo.orientation == Orientation.Horizontal
            val gridWidth = gridLayoutInfo.viewportSize.width
            val gridHeight = gridLayoutInfo.viewportSize.height
            val reversedProgress = 1f - progress
            if (isHorizontal) {
                translationX = if (bounds.right <= zoomCentroid.x) {
                    // Move from left
                    -bounds.right * reversedProgress
                } else {
                    // Move from right
                    (gridWidth - bounds.left) * reversedProgress
                }
            } else {
                translationY = if (bounds.bottom <= zoomCentroid.y) {
                    // Move from top
                    -bounds.bottom * reversedProgress
                } else {
                    // Move from bottom
                    (gridHeight - bounds.top) * reversedProgress
                }
            }
        }
    }
}

private fun LayoutCoordinates.bounds(): Rect {
    return Rect(
        offset = positionInRoot(),
        size = size.toSize(),
    )
}

private fun Modifier.onDetach(block: () -> Unit): Modifier =
    this.then(OnDetachModifierElement(block))

private class OnDetachModifierElement(
    private var onDetach: () -> Unit,
) : ModifierNodeElement<OnDetachModifierNode>() {
    override fun InspectorInfo.inspectableProperties() {
        name = "onDetach"
        properties["onDetach"] = onDetach
    }

    override fun create(): OnDetachModifierNode {
        return OnDetachModifierNode(onDetach)
    }

    override fun update(node: OnDetachModifierNode) {
        node.onDetach = onDetach
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OnDetachModifierElement

        return onDetach == other.onDetach
    }

    override fun hashCode(): Int {
        return onDetach.hashCode()
    }
}

private class OnDetachModifierNode(
    var onDetach: () -> Unit,
) : Modifier.Node() {
    override fun onDetach() {
        super.onDetach()
        this.onDetach.invoke()
    }
}