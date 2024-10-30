package com.ismartcoding.plain.ui.components.mediaviewer.previewer

import android.net.Uri
import android.widget.ImageView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import com.ismartcoding.lib.extensions.isImageFast
import com.ismartcoding.plain.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// 用于操作transformItemStateMap的锁对象
internal val imageTransformMutex = Mutex()

// 用于缓存界面上的transformItemState
internal val transformItemStateMap = mutableStateMapOf<Any, TransformItemState>()

@Composable
fun TransformImageView(
    modifier: Modifier = Modifier,
    path: String,
    key: String,
    uri: Uri? = null,
    itemState: TransformItemState,
    previewerState: MediaPreviewerState,
    widthPx: Int,
) {
    TransformImageView(
        modifier = modifier,
        key = key,
        itemState = itemState,
        contentState = previewerState.transformState,
    ) { itemKey ->
        key(itemKey) {
            val imageModifier = Modifier
                .fillMaxSize()
            val painter = rememberAsyncImagePainter(
                model = ImageRequest
                    .Builder(LocalContext.current)
                    .data(uri ?: path)
                    .size(widthPx)
                    .build(),
                filterQuality = FilterQuality.None
            )

            if (painter.state.value is AsyncImagePainter.State.Error) {
                Image(
                    modifier = imageModifier,
                    painter = painterResource(if (path.isImageFast()) R.drawable.image else R.drawable.file_video),
                    contentDescription = path,
                    contentScale = ContentScale.Crop,
                )
            } else {
                Image(
                    modifier = if (path.endsWith(".svg", true)) imageModifier.background(Color.White) else imageModifier,
                    painter = painter,
                    contentDescription = path,
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

@Composable
fun TransformImageView(
    modifier: Modifier = Modifier,
    key: Any,
    itemState: TransformItemState = rememberTransformItemState(),
    previewerState: MediaPreviewerState,
    content: @Composable (Any) -> Unit,
) = TransformImageView(modifier, key, itemState, previewerState.transformState, content)

@Composable
fun TransformImageView(
    modifier: Modifier = Modifier,
    key: Any,
    itemState: TransformItemState = rememberTransformItemState(),
    contentState: TransformContentState? = rememberTransformContentState(),
    content: @Composable (Any) -> Unit,
) {
    TransformItemView(
        modifier = modifier,
        key = key,
        itemState = itemState,
        contentState = contentState,
    ) {
        content(key)
    }
}

@Composable
fun TransformItemView(
    modifier: Modifier = Modifier,
    key: Any,
    itemState: TransformItemState = rememberTransformItemState(),
    contentState: TransformContentState?,
    content: @Composable (Any) -> Unit,
) {
    val scope = rememberCoroutineScope()
    itemState.key = key
    itemState.blockCompose = content
    DisposableEffect(key) {
        // 这个composable加载时添加到map
        scope.launch {
            itemState.addItem()
        }
        onDispose {
            // composable退出时从map移除
            itemState.removeItem()
        }
    }
    Box(
        modifier = modifier
            .onGloballyPositioned {
                itemState.onPositionChange(
                    position = it.positionInRoot(),
                    size = it.size,
                )
            }
            .fillMaxSize()
    ) {
        if (
            contentState?.itemState != itemState || !contentState.onAction
        ) {
            itemState.blockCompose(key)
        }
    }
}

@Composable
fun TransformContentView(
    transformContentState: TransformContentState = rememberTransformContentState(),
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                transformContentState.containerSize = it.size
                transformContentState.containerOffset = it.positionInRoot()
            },
    ) {
        if (
            transformContentState.srcCompose != null
            && transformContentState.onAction
        ) {
            Box(
                modifier = Modifier
                    .offset(
                        x = LocalDensity.current.run { (transformContentState.offsetX.value).toDp() },
                        y = LocalDensity.current.run { (transformContentState.offsetY.value).toDp() },
                    )
                    .size(
                        width = LocalDensity.current.run { transformContentState.displayWidth.value.toDp() },
                        height = LocalDensity.current.run { transformContentState.displayHeight.value.toDp() },
                    )
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0F, 0F)
                        scaleX = transformContentState.graphicScaleX.value
                        scaleY = transformContentState.graphicScaleY.value
                    },
            ) {
                transformContentState.srcCompose!!(transformContentState.itemState?.key ?: Unit)
            }
        }
    }
}

class TransformContentState(
    // 协程作用域
    var scope: CoroutineScope = MainScope(),
    // 默认动画窗格
    var defaultAnimationSpec: AnimationSpec<Float> = DEFAULT_SOFT_ANIMATION_SPEC
) {

    var itemState: TransformItemState? by mutableStateOf(null)

    private val intrinsicRatio: Float
        get() {
            val intrinsicSize = itemState?.intrinsicSize ?: Size.Zero
            if (intrinsicSize.height == 0F) return 1F
            return intrinsicSize.width.div(intrinsicSize.height)
        }

    private val srcPosition: Offset
        get() {
            val offset = itemState?.blockPosition ?: Offset.Zero
            return offset.copy(x = offset.x - containerOffset.x, y = offset.y - containerOffset.y)
        }

    private val srcSize: IntSize
        get() = itemState?.blockSize ?: IntSize.Zero

    val srcCompose: (@Composable (Any) -> Unit)?
        get() = itemState?.blockCompose

    var onAction by mutableStateOf(false)

    private var onActionTarget by mutableStateOf<Boolean?>(null)

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
                scope.launch {
                    specifierSizeFlow.emit(true)
                }
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
                // 宽度一致
                val uW = containerSize.width
                val uH = uW / intrinsicRatio
                Size(uW.toFloat(), uH)
            } else {
                // 高度一致
                val uH = containerSize.height
                val uW = uH * intrinsicRatio
                Size(uW, uH.toFloat())
            }
        }

    private val fitOffsetX: Float
        get() {
            return (containerSize.width - fitSize.width).div(2)
        }

    private val fitOffsetY: Float
        get() {
            return (containerSize.height - fitSize.height).div(2)
        }

    val fitScale: Float
        get() {
            return fitSize.width.div(displayRatioSize.width)
        }

    val displayRatioSize: Size
        get() {
            return Size(width = srcSize.width.toFloat(), height = srcSize.width.div(intrinsicRatio))
        }

    val realSize: Size
        get() {
            return Size(
                width = displayWidth.value * graphicScaleX.value,
                height = displayHeight.value * graphicScaleY.value,
            )
        }

    suspend fun awaitContainerSizeSpecifier() {
        specifierSizeFlow.takeWhile { !it }.collect {}
    }

    fun findTransformItem(key: Any) = transformItemStateMap[key]

    fun clearTransformItems() = transformItemStateMap.clear()

    fun setEnterState() {
        onAction = true
        onActionTarget = null
    }

    fun setExitState() {
        onAction = false
        onActionTarget = null
    }

    suspend fun notifyEnterChanged() {
        scope.launch {
            listOf(
                scope.async {
                    displayWidth.snapTo(displayRatioSize.width)
                },
                scope.async {
                    displayHeight.snapTo(displayRatioSize.height)
                },
                scope.async {
                    graphicScaleX.snapTo(fitScale)
                },
                scope.async {
                    graphicScaleY.snapTo(fitScale)
                },
                scope.async {
                    offsetX.snapTo(fitOffsetX)
                },
                scope.async {
                    offsetY.snapTo(fitOffsetY)
                },
            ).awaitAll()
        }
    }

    suspend fun exitTransform(
        animationSpec: AnimationSpec<Float>? = null
    ) = suspendCoroutine<Unit> { c ->
        val currentAnimateSpec = animationSpec ?: defaultAnimationSpec
        scope.launch {
            listOf(
                scope.async {
                    displayWidth.animateTo(srcSize.width.toFloat(), currentAnimateSpec)
                },
                scope.async {
                    displayHeight.animateTo(srcSize.height.toFloat(), currentAnimateSpec)
                },
                scope.async {
                    graphicScaleX.animateTo(1F, currentAnimateSpec)
                },
                scope.async {
                    graphicScaleY.animateTo(1F, currentAnimateSpec)
                },
                scope.async {
                    offsetX.animateTo(srcPosition.x, currentAnimateSpec)
                },
                scope.async {
                    offsetY.animateTo(srcPosition.y, currentAnimateSpec)
                },
            ).awaitAll()
            onAction = false
            onActionTarget = null
            c.resume(Unit)
        }
    }

    suspend fun enterTransform(
        itemState: TransformItemState,
        animationSpec: AnimationSpec<Float>? = null
    ) = suspendCoroutine<Unit> { c ->
        val currentAnimationSpec = animationSpec ?: defaultAnimationSpec
        this.itemState = itemState

        displayWidth = Animatable(srcSize.width.toFloat())
        displayHeight = Animatable(srcSize.height.toFloat())
        graphicScaleX = Animatable(1F)
        graphicScaleY = Animatable(1F)

        offsetX = Animatable(srcPosition.x)
        offsetY = Animatable(srcPosition.y)

        onActionTarget = true
        onAction = true

        scope.launch {
            reset(currentAnimationSpec)
            c.resume(Unit)
            onActionTarget = null
        }
    }

    suspend fun reset(animationSpec: AnimationSpec<Float>? = null) {
        val currentAnimationSpec = animationSpec ?: defaultAnimationSpec
        listOf(
            scope.async {
                displayWidth.animateTo(displayRatioSize.width, currentAnimationSpec)
            },
            scope.async {
                displayHeight.animateTo(displayRatioSize.height, currentAnimationSpec)
            },
            scope.async {
                graphicScaleX.animateTo(fitScale, currentAnimationSpec)
            },
            scope.async {
                graphicScaleY.animateTo(fitScale, currentAnimationSpec)
            },
            scope.async {
                offsetX.animateTo(fitOffsetX, currentAnimationSpec)
            },
            scope.async {
                offsetY.animateTo(fitOffsetY, currentAnimationSpec)
            },
        ).awaitAll()
    }

    companion object {
        val Saver: Saver<TransformContentState, *> = listSaver(
            save = {
                listOf<Any>(
                    it.onAction,
                )
            },
            restore = {
                val transformContentState = TransformContentState()
                transformContentState.onAction = it[0] as Boolean
                transformContentState
            }
        )
    }

}

@Composable
fun rememberTransformContentState(
    scope: CoroutineScope = rememberCoroutineScope(),
    animationSpec: AnimationSpec<Float> = DEFAULT_SOFT_ANIMATION_SPEC
): TransformContentState {
    val transformContentState = rememberSaveable(saver = TransformContentState.Saver) {
        TransformContentState()
    }
    transformContentState.scope = scope
    transformContentState.defaultAnimationSpec = animationSpec
    return transformContentState
}

class TransformItemState(
    var key: Any = Unit,
    var blockCompose: (@Composable (Any) -> Unit) = {},
    var scope: CoroutineScope,
    var blockPosition: Offset = Offset.Zero,
    var blockSize: IntSize = IntSize.Zero,
    var intrinsicSize: Size? = null,
    var checkInBound: (TransformItemState.() -> Boolean)? = null,
) {

    private fun checkItemInMap() {
        if (checkInBound == null) return
        if (checkInBound!!.invoke(this)) {
            addItem()
        } else {
            removeItem()
        }
    }

    /**
     * 位置和大小发生变化时
     * @param position Offset
     * @param size IntSize
     */
    internal fun onPositionChange(position: Offset, size: IntSize) {
        blockPosition = position
        blockSize = size
        scope.launch {
            checkItemInMap()
        }
    }

    /**
     * 判断item是否在所需范围内，返回true，则添加该item到map，返回false则移除
     * @param checkInBound Function0<Boolean>
     */
    fun checkIfInBound(checkInBound: () -> Boolean) {
        if (checkInBound()) {
            addItem()
        } else {
            removeItem()
        }
    }

    /**
     * 添加item到map上
     * @param key Any?
     */
    fun addItem(key: Any? = null) {
        val currentKey = key ?: this.key
        if (checkInBound != null) return
        synchronized(imageTransformMutex) {
            transformItemStateMap[currentKey] = this
        }
    }

    /**
     * 从map上移除item
     * @param key Any?
     */
    fun removeItem(key: Any? = null) {
        synchronized(imageTransformMutex) {
            val currentKey = key ?: this.key
            if (checkInBound != null) return
            transformItemStateMap.remove(currentKey)
        }
    }
}

@Composable
fun rememberTransformItemState(
    scope: CoroutineScope = rememberCoroutineScope(),
    checkInBound: (TransformItemState.() -> Boolean)? = null,
): TransformItemState {
    return remember { TransformItemState(scope = scope, checkInBound = checkInBound) }
}