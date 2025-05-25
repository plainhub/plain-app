package com.ismartcoding.plain.ui.components.mediaviewer

import androidx.compose.animation.core.FloatExponentialDecaySpec
import androidx.compose.animation.core.generateDecayAnimationSpec
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import com.ismartcoding.lib.extensions.isUrl
import com.ismartcoding.lib.extensions.isVideoFast
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.ui.components.mediaviewer.hugeimage.ImageDecoder
import com.ismartcoding.plain.ui.components.mediaviewer.hugeimage.MediaHugeImage
import com.ismartcoding.plain.ui.components.mediaviewer.video.MediaVideo
import com.ismartcoding.plain.ui.components.mediaviewer.video.VideoState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.absoluteValue

// 默认X轴偏移量
const val DEFAULT_OFFSET_X = 0F

// 默认Y轴偏移量
const val DEFAULT_OFFSET_Y = 0F

// 默认缩放率
const val DEFAULT_SCALE = 1F

// 默认旋转角度
const val DEFAULT_ROTATION = 0F

// 图片最小缩放率
const val MIN_SCALE = 0.5F

// 图片最大缩放率
const val MAX_SCALE_RATE = 3.2F

// 最小手指手势间距
const val MIN_GESTURE_FINGER_DISTANCE = 200

class GestureScope(
    var onTap: (Offset) -> Unit = {},
    var onDoubleTap: (Offset) -> Boolean = { false },
    var onLongPress: (Offset) -> Unit = {},
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaViewer(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    videoState: VideoState,
    page: Int,
    model: Any?,
    state: MediaViewerState = rememberViewerState(),
    gesture: GestureScope,
    boundClip: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    // 触摸时中心位置
    var centroid by remember { mutableStateOf(Offset.Zero) }
    // 减速运动动画曲线
    val decay = remember {
        FloatExponentialDecaySpec(2f).generateDecayAnimationSpec<Float>()
    }
    var velocityTracker = remember { VelocityTracker() }
    // 记录触摸事件中手指的个数
    var eventChangeCount by remember { mutableIntStateOf(0) }
    // 最后一次偏移运动
    var lastPan by remember { mutableStateOf(Offset.Zero) }
    // 手势实时的偏移范围
    var boundX by remember { mutableFloatStateOf(0F) }
    var boundY by remember { mutableFloatStateOf(0F) }
    // 最大缩放率，双击的时候会放大到这个值
    var maxScale by remember { mutableFloatStateOf(1F) }
    // 最大显示缩放率，缩放率超过这个值后，手势结束了就会自动恢复到这个值
    val maxDisplayScale by remember { derivedStateOf { maxScale * MAX_SCALE_RATE } }
    // 目标偏移量
    var desX by remember { mutableFloatStateOf(0F) }
    var desY by remember { mutableFloatStateOf(0F) }
    // 目标缩放率
    var desScale by remember { mutableFloatStateOf(1F) }
    // 缩放率修改前的值
    var fromScale by remember { mutableFloatStateOf(1F) }
    // 计算边界使用的缩放率
    var boundScale by remember { mutableFloatStateOf(1F) }
    // 目标旋转角度
    var desRotation by remember { mutableFloatStateOf(0F) }
    // 要增加的旋转角度
    var rotate by remember { mutableFloatStateOf(0F) }
    // 要增加的放大倍率
    var zoom by remember { mutableFloatStateOf(1F) }
    // 两个手指的距离
    var fingerDistanceOffset by remember { mutableStateOf(Offset.Zero) }

    // 同步des的参数，在gallery的图片切换时，缩小后仍然接收手势指令，所以需要同步缩小后的参数
    fun asyncDesParams() {
        desX = state.offsetX.value
        desY = state.offsetY.value
        desScale = state.scale.value
        desRotation = state.rotation.value
    }
    LaunchedEffect(key1 = state.resetTimeStamp) {
        asyncDesParams()
    }
    val rawGesture = remember {
        RawGesture(
            onTap = gesture.onTap,
            onDoubleTap = { gesture.onDoubleTap(it) },
            onLongPress = gesture.onLongPress,
            gestureStart = {
                if (state.allowGestureInput) {
                    eventChangeCount = 0
                    velocityTracker = VelocityTracker()
                    scope.launch {
                        state.offsetX.stop()
                        state.offsetY.stop()
                        state.offsetX.updateBounds(null, null)
                        state.offsetY.updateBounds(null, null)
                    }
                    asyncDesParams()
                }
            },
            gestureEnd = { transformOnly ->
                // transformOnly记录手势事件中是否有位移，如果只是点击或双击，会返回false
                // 如果正在动画中，就不要执行后续动作，如：reset指令执行时
                if (transformOnly && !state.isRunning() && state.allowGestureInput) {
                    // 处理加速度添加的点为空的情况
                    var velocity = try {
                        velocityTracker.calculateVelocity()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        LogCat.e(e.toString())
                        null
                    }
                    // 如果缩放比小于1，要自动回到1
                    // 如果缩放比大于最大显示缩放比，就设置回去，并且避免加速度
                    val scale = when {
                        state.scale.value < 1 -> 1F
                        state.scale.value > maxDisplayScale -> {
                            velocity = null
                            maxDisplayScale
                        }

                        else -> null
                    }
                    // 如果此时位移超出范围，就动画回范围内
                    // 如果没超出范围，就设置animate的范围，然后执行抛掷动画
                    scope.launch {
                        if (inBound(state.offsetX.value, boundX) && velocity != null) {
                            val vx = sameDirection(lastPan.x, velocity.x)
                            state.offsetX.updateBounds(-boundX, boundX)
                            state.offsetX.animateDecay(vx, decay)
                        } else {
                            val targetX = if (scale != maxDisplayScale) {
                                limitToBound(state.offsetX.value, boundX)
                            } else {
                                panTransformAndScale(
                                    offset = state.offsetX.value,
                                    center = centroid.x,
                                    bh = state.containerSize.width.toFloat(),
                                    uh = state.defaultSize.width.toFloat(),
                                    fromScale = state.scale.value,
                                    toScale = scale,
                                )
                            }
                            state.offsetX.animateTo(targetX)
                        }
                    }
                    scope.launch {
                        if (inBound(state.offsetY.value, boundY) && velocity != null) {
                            val vy = sameDirection(lastPan.y, velocity.y)
                            state.offsetY.updateBounds(-boundY, boundY)
                            state.offsetY.animateDecay(vy, decay)
                        } else {
                            val targetY = if (scale != maxDisplayScale) {
                                limitToBound(state.offsetY.value, boundY)
                            } else {
                                panTransformAndScale(
                                    offset = state.offsetY.value,
                                    center = centroid.y,
                                    bh = state.containerSize.height.toFloat(),
                                    uh = state.defaultSize.height.toFloat(),
                                    fromScale = state.scale.value,
                                    toScale = scale,
                                )
                            }
                            state.offsetY.animateTo(targetY)
                        }
                    }
                    // scope.launch {
                    // state.rotation.animateTo(0F)
                    //  }
                    scale?.let {
                        scope.launch {
                            state.scale.animateTo(scale)
                        }
                    }
                }
            },
        ) { center, pan, _zoom, _rotate, event ->
            // 当禁止手势输入时
            if (!state.allowGestureInput) return@RawGesture true
            // 这里只记录最大手指数
            if (event.changes.size > eventChangeCount) eventChangeCount = event.changes.size
            // 如果手指数从多个变成一个，就结束本次手势操作
            if (eventChangeCount > event.changes.size) return@RawGesture false

            rotate = _rotate
            zoom = _zoom
            // 如果是双指的情况下，手指距离小于一定值时，缩放和旋转的值会很离谱，所以在这种极端情况下就不要处理缩放和旋转了
            if (event.changes.size == 2) {
                fingerDistanceOffset = event.changes[0].position - event.changes[1].position
                if (
                    fingerDistanceOffset.x.absoluteValue < MIN_GESTURE_FINGER_DISTANCE
                    && fingerDistanceOffset.y.absoluteValue < MIN_GESTURE_FINGER_DISTANCE
                ) {
                    rotate = 0F
                    zoom = 1F
                }
            }
            // 上一次的偏移量
            lastPan = pan
            // 记录手势的中点
            centroid = center
            // 记录当前缩放比
            fromScale = desScale
            // 目标放大倍率
            desScale *= zoom
            // 检查最小放大倍率
            if (desScale < MIN_SCALE) desScale = MIN_SCALE

            // 计算边界，如果目标缩放值超过最大显示缩放值，边界就要用最大缩放值来计算，否则手势结束时会导致无法归位
            boundScale = if (desScale > maxDisplayScale) maxDisplayScale else desScale
            boundX =
                getBound(boundScale * state.defaultSize.width, state.containerSize.width.toFloat())
            boundY =
                getBound(
                    boundScale * state.defaultSize.height,
                    state.containerSize.height.toFloat()
                )

            desX = panTransformAndScale(
                offset = desX,
                center = center.x,
                bh = state.containerSize.width.toFloat(),
                uh = state.defaultSize.width.toFloat(),
                fromScale = fromScale,
                toScale = desScale,
            ) + pan.x
            // 如果手指数1，就是拖拽，拖拽受范围限制
            // 如果手指数大于1，即有缩放事件，则支持中心点放大
            if (eventChangeCount == 1) desX = limitToBound(desX, boundX)
            desY = panTransformAndScale(
                offset = desY,
                center = center.y,
                bh = state.containerSize.height.toFloat(),
                uh = state.defaultSize.height.toFloat(),
                fromScale = fromScale,
                toScale = desScale,
            ) + pan.y
            if (eventChangeCount == 1) desY = limitToBound(desY, boundY)

            if (desScale < 1) desRotation += rotate
            velocityTracker.addPosition(
                event.changes[0].uptimeMillis,
                Offset(desX, desY),
            )
            if (!state.isRunning()) scope.launch {
                state.scale.snapTo(desScale)
                state.offsetX.snapTo(desX)
                state.offsetY.snapTo(desY)
                // state.rotation.snapTo(desRotation)
            }

            // 这里判断是否已运动到边界，如果到了边界，就不消费事件，让上层界面获取到事件
            val onLeft = desX >= boundX
            val onRight = desX <= -boundX
            val reachSide = !(onLeft && pan.x > 0)
                    && !(onRight && pan.x < 0)
                    && !(onLeft && onRight)
            if (reachSide || state.scale.value < 1) {
                event.changes.fastForEach {
                    if (it.positionChanged()) {
                        it.consume()
                    }
                }
            }

            // 返回true，继续下一次手势
            return@RawGesture true
        }
    }
    val sizeChange: suspend (SizeChangeContent) -> Unit = { content ->
        maxScale = content.maxScale
        state.defaultSize = content.defaultSize
        state.containerSize = content.containerSize
        state.maxScale = content.maxScale
        if (state.fromSaver) {
            state.fromSaver = false
            state.fixToBound()
        }
    }
    Box(modifier = modifier) {
        val onMounted: () -> Unit = {
            scope.launch {
                state.mountedFlow.emit(true)
            }
        }

        when (model) {
            is PreviewItem,
            -> {
                if (model.path.isVideoFast() && !model.path.isUrl()) {
                    MediaVideo(
                        pagerState = pagerState,
                        videoState = videoState,
                        page = page,
                        model = model,
                        scale = state.scale.value,
                        offsetX = state.offsetX.value,
                        offsetY = state.offsetY.value,
                        rotation = state.rotation.value,
                        gesture = rawGesture,
                        onMounted = onMounted,
                        onSizeChange = sizeChange,
                        boundClip = boundClip,
                    )
                } else {
                    MediaNormalImage(
                        model = model,
                        scale = state.scale.value,
                        offsetX = state.offsetX.value,
                        offsetY = state.offsetY.value,
                        rotation = state.rotation.value,
                        gesture = rawGesture,
                        onSizeChange = sizeChange,
                        onMounted = onMounted,
                        boundClip = boundClip,
                    )
                }
            }

            is ImageDecoder -> {
                MediaHugeImage(
                    imageDecoder = model,
                    scale = state.scale.value,
                    offsetX = state.offsetX.value,
                    offsetY = state.offsetY.value,
                    rotation = state.rotation.value,
                    gesture = rawGesture,
                    onSizeChange = sizeChange,
                    onMounted = onMounted,
                    boundClip = boundClip,
                )
            }
        }
    }
}

/**
 * 重写事件监听方法
 */
suspend fun PointerInputScope.detectTransformGestures(
    panZoomLock: Boolean = false,
    gestureStart: () -> Unit = {},
    gestureEnd: (Boolean) -> Unit = {},
    onTap: (Offset) -> Unit = {},
    onDoubleTap: (Offset) -> Unit = {},
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float, event: PointerEvent) -> Boolean,
) {
    var lastReleaseTime = 0L
    var scope: CoroutineScope? = null
    awaitEachGesture {
        var rotation = 0f
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop
        var lockedToPanZoom = false

        awaitFirstDown(requireUnconsumed = false)
        val t0 = System.currentTimeMillis()
        var releasedEvent: PointerEvent? = null
        var moveCount = 0

        gestureStart()
        do {
            val event = awaitPointerEvent()
            if (event.type == PointerEventType.Release) releasedEvent = event
            if (event.type == PointerEventType.Move) moveCount++
            val canceled = event.changes.fastAny { it.isConsumed }
            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val rotationChange = event.calculateRotation()
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    rotation += rotationChange
                    pan += panChange

                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1 - zoom) * centroidSize
                    val rotationMotion = abs(rotation * PI.toFloat() * centroidSize / 180f)
                    val panMotion = pan.getDistance()

                    if (zoomMotion > touchSlop ||
                        rotationMotion > touchSlop ||
                        panMotion > touchSlop
                    ) {
                        pastTouchSlop = true
                        lockedToPanZoom = panZoomLock && rotationMotion < touchSlop
                    }
                }
                if (pastTouchSlop) {
                    val centroid = event.calculateCentroid(useCurrent = false)
                    val effectiveRotation = if (lockedToPanZoom) 0f else rotationChange
                    if (effectiveRotation != 0f ||
                        zoomChange != 1f ||
                        panChange != Offset.Zero
                    ) {
                        if (!onGesture(
                                centroid,
                                panChange,
                                zoomChange,
                                effectiveRotation,
                                event
                            )
                        ) break
                    }
                }
            }
        } while (!canceled && event.changes.fastAny { it.pressed })

        var t1 = System.currentTimeMillis()
        val dt = t1 - t0
        val dlt = t1 - lastReleaseTime

        if (moveCount == 0) releasedEvent?.let { e ->
            if (e.changes.isEmpty()) return@let
            val offset = e.changes.first().position
            if (dlt < 272) {
                t1 = 0L
                scope?.cancel()
                onDoubleTap(offset)
            } else if (dt < 200) {
                scope = MainScope()
                scope?.launch(Dispatchers.Main) {
                    delay(272)
                    onTap(offset)
                }
            }
            lastReleaseTime = t1
        }

        gestureEnd(moveCount != 0)
    }
}

/**
 * 让后一个数与前一个数的符号保持一致
 * @param a Float
 * @param b Float
 * @return Float
 */
fun sameDirection(a: Float, b: Float): Float {
    return if (a > 0) {
        if (b < 0) {
            b.absoluteValue
        } else {
            b
        }
    } else {
        if (b > 0) {
            -b
        } else {
            b
        }
    }
}

/**
 * 获取移动边界
 */
fun getBound(rw: Float, bw: Float): Float {
    return if (rw > bw) {
        var xb = (rw - bw).div(2)
        if (xb < 0) xb = 0F
        xb
    } else {
        0F
    }
}

/**
 * 判断位移是否在边界内
 */
fun inBound(offset: Float, bound: Float): Boolean {
    return if (offset > 0) {
        offset < bound
    } else if (offset < 0) {
        offset > -bound
    } else {
        true
    }
}

/**
 * 把位移限制在边界内
 */
fun limitToBound(offset: Float, bound: Float): Float {
    return when {
        offset > bound -> {
            bound
        }

        offset < -bound -> {
            -bound
        }

        else -> {
            offset
        }
    }
}

/**
 * 追踪缩放过程中的中心点
 */
fun panTransformAndScale(
    offset: Float,
    center: Float,
    bh: Float,
    uh: Float,
    fromScale: Float,
    toScale: Float,
): Float {
    val srcH = uh * fromScale
    val desH = uh * toScale
    val gapH = (bh - uh) / 2

    val py = when {
        uh >= bh -> {
            val upy = (uh * fromScale - uh).div(2)
            (upy - offset + center) / (fromScale * uh)
        }

        srcH > bh || bh > uh -> {
            val upy = (srcH - uh).div(2)
            (upy - gapH - offset + center) / (fromScale * uh)
        }

        else -> {
            val upy = -(bh - srcH).div(2)
            (upy - offset + center) / (fromScale * uh)
        }
    }
    return when {
        uh >= bh -> {
            val upy = (uh * toScale - uh).div(2)
            upy + center - py * toScale * uh
        }

        desH > bh -> {
            val upy = (desH - uh).div(2)
            upy - gapH + center - py * toScale * uh
        }

        else -> {
            val upy = -(bh - desH).div(2)
            upy + center - py * desH
        }
    }
}
