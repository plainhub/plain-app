package com.ismartcoding.plain.ui.components.mediaviewer.previewer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch

class TransformContentState(
    var scope: CoroutineScope = MainScope(),
    var defaultAnimationSpec: AnimationSpec<Float> = DEFAULT_SOFT_ANIMATION_SPEC
) {
    var itemState: TransformItemState? by mutableStateOf(null)

    private val intrinsicRatio: Float
        get() {
            val intrinsicSize = itemState?.intrinsicSize ?: Size.Zero
            if (intrinsicSize.height == 0F) return 1F
            return intrinsicSize.width.div(intrinsicSize.height)
        }

    internal val srcPosition: Offset
        get() {
            val offset = itemState?.blockPosition ?: Offset.Zero
            return offset.copy(x = offset.x - containerOffset.x, y = offset.y - containerOffset.y)
        }

    internal val srcSize: IntSize
        get() = itemState?.blockSize ?: IntSize.Zero

    val srcCompose: (@Composable (Any) -> Unit)?
        get() = itemState?.blockCompose

    var onAction by mutableStateOf(false)

    internal var onActionTarget by mutableStateOf<Boolean?>(null)

    var displayWidth = Animatable(0F)
    var displayHeight = Animatable(0F)
    var graphicScaleX = Animatable(1F)
    var graphicScaleY = Animatable(1F)
    var offsetX = Animatable(0F)
    var offsetY = Animatable(0F)

    var containerOffset by mutableStateOf(Offset.Zero)

    private var containerSizeState = mutableStateOf(IntSize.Zero)

    var containerSize: IntSize
        get() = containerSizeState.value
        set(value) {
            containerSizeState.value = value
            if (value.width != 0 && value.height != 0) {
                scope.launch { specifierSizeFlow.emit(true) }
            }
        }

    private var specifierSizeFlow = MutableStateFlow(false)

    private val containerRatio: Float
        get() {
            if (containerSize.height == 0) return 1F
            return containerSize.width.toFloat().div(containerSize.height)
        }

    val widthFixed: Boolean
        get() = intrinsicRatio > containerRatio

    val fitSize: Size
        get() {
            return if (intrinsicRatio > containerRatio) {
                val uW = containerSize.width; val uH = uW / intrinsicRatio
                Size(uW.toFloat(), uH)
            } else {
                val uH = containerSize.height; val uW = uH * intrinsicRatio
                Size(uW, uH.toFloat())
            }
        }

    internal val fitOffsetX: Float
        get() = (containerSize.width - fitSize.width).div(2)

    internal val fitOffsetY: Float
        get() = (containerSize.height - fitSize.height).div(2)

    val fitScale: Float
        get() = fitSize.width.div(displayRatioSize.width)

    val displayRatioSize: Size
        get() = Size(width = srcSize.width.toFloat(), height = srcSize.width.div(intrinsicRatio))

    val realSize: Size
        get() = Size(
            width = displayWidth.value * graphicScaleX.value,
            height = displayHeight.value * graphicScaleY.value,
        )

    suspend fun awaitContainerSizeSpecifier() {
        specifierSizeFlow.takeWhile { !it }.collect {}
    }

    fun findTransformItem(key: Any) = transformItemStateMap[key]
    fun clearTransformItems() = transformItemStateMap.clear()

    fun setEnterState() { onAction = true; onActionTarget = null }
    fun setExitState() { onAction = false; onActionTarget = null }

    companion object {
        val Saver: Saver<TransformContentState, *> = listSaver(
            save = { listOf<Any>(it.onAction) },
            restore = {
                val s = TransformContentState(); s.onAction = it[0] as Boolean; s
            }
        )
    }
}

@Composable
fun rememberTransformContentState(
    scope: CoroutineScope = rememberCoroutineScope(),
    animationSpec: AnimationSpec<Float> = DEFAULT_SOFT_ANIMATION_SPEC
): TransformContentState {
    val state = rememberSaveable(saver = TransformContentState.Saver) { TransformContentState() }
    state.scope = scope; state.defaultAnimationSpec = animationSpec
    return state
}
