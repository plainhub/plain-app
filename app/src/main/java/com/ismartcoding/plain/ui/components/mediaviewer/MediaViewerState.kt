package com.ismartcoding.plain.ui.components.mediaviewer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MediaViewerState(
    // X轴偏移量
    offsetX: Float = DEFAULT_OFFSET_X,
    // Y轴偏移量
    offsetY: Float = DEFAULT_OFFSET_Y,
    // 缩放率
    scale: Float = DEFAULT_SCALE,
    // 旋转角度
    rotation: Float = DEFAULT_ROTATION,
) : CoroutineScope by MainScope() {

    // 默认动画窗格
    private var defaultAnimateSpec: AnimationSpec<Float> = SpringSpec()

    // x偏移
    val offsetX = Animatable(offsetX)

    // y偏移
    val offsetY = Animatable(offsetY)

    // 放大倍率
    val scale = Animatable(scale)

    // 旋转
    val rotation = Animatable(rotation)

    // 是否允许手势输入
    var allowGestureInput by mutableStateOf(true)

    // 默认显示大小
    var defaultSize by mutableStateOf(IntSize(0, 0))
        internal set

    // 容器大小
    internal var containerSize by mutableStateOf(IntSize(0, 0))

    // 最大缩放
    internal var maxScale by mutableFloatStateOf(1F)

    // 标识是否来自saver，旋转屏幕后会变成true
    internal var fromSaver = false

    // 恢复的时间戳
    internal var resetTimeStamp by mutableLongStateOf(0L)

    // 挂载状态
    internal val mountedFlow = MutableStateFlow(false)

    /**
     * 判断是否有动画正在运行
     * @return Boolean
     */
    internal fun isRunning(): Boolean {
        return scale.isRunning
                || offsetX.isRunning
                || offsetY.isRunning
                || rotation.isRunning
    }

    /**
     * 立即设置回初始值
     */
    suspend fun resetImmediately() {
        rotation.snapTo(DEFAULT_ROTATION)
        offsetX.snapTo(DEFAULT_OFFSET_X)
        offsetY.snapTo(DEFAULT_OFFSET_Y)
        scale.snapTo(DEFAULT_SCALE)
    }

    /**
     * 设置回初始值
     */
    suspend fun reset(animationSpec: AnimationSpec<Float> = defaultAnimateSpec) {
        coroutineScope {
            launch {
                rotation.animateTo(DEFAULT_ROTATION, animationSpec)
                resetTimeStamp = System.currentTimeMillis()
            }
            launch {
                offsetX.animateTo(DEFAULT_OFFSET_X, animationSpec)
                resetTimeStamp = System.currentTimeMillis()
            }
            launch {
                offsetY.animateTo(DEFAULT_OFFSET_Y, animationSpec)
                resetTimeStamp = System.currentTimeMillis()
            }
            launch {
                scale.animateTo(DEFAULT_SCALE, animationSpec)
                resetTimeStamp = System.currentTimeMillis()
            }
        }
    }

    /**
     * 放大到最大
     */
    private suspend fun scaleToMax(
        offset: Offset,
        animationSpec: AnimationSpec<Float>? = null
    ) {
        val currentAnimateSpec = animationSpec ?: defaultAnimateSpec
        // 计算x和y偏移量和范围，并确保不会在放大过程中超出范围
        var bcx = (containerSize.width / 2 - offset.x) * maxScale
        val boundX = getBound(defaultSize.width.toFloat() * maxScale, containerSize.width.toFloat())
        bcx = limitToBound(bcx, boundX)
        var bcy = (containerSize.height / 2 - offset.y) * maxScale
        val boundY =
            getBound(defaultSize.height.toFloat() * maxScale, containerSize.height.toFloat())
        bcy = limitToBound(bcy, boundY)
        // 启动
        coroutineScope {
            launch {
                scale.animateTo(maxScale, currentAnimateSpec)
            }
            launch {
                offsetX.animateTo(bcx, currentAnimateSpec)
            }
            launch {
                offsetY.animateTo(bcy, currentAnimateSpec)
            }
        }
    }

    /**
     * 放大或缩小
     */
    suspend fun toggleScale(
        offset: Offset,
        animationSpec: AnimationSpec<Float> = defaultAnimateSpec
    ) {
        // 如果不等于1，就调回1
        if (scale.value != 1F) {
            reset(animationSpec)
        } else {
            scaleToMax(offset, animationSpec)
        }
    }

    /**
     * 修正offsetX,offsetY的位置
     */
    suspend fun fixToBound() {
        val boundX =
            getBound(defaultSize.width.toFloat() * scale.value, containerSize.width.toFloat())
        val boundY =
            getBound(defaultSize.height.toFloat() * scale.value, containerSize.height.toFloat())
        val limitX = limitToBound(offsetX.value, boundX)
        val limitY = limitToBound(offsetY.value, boundY)
        offsetX.snapTo(limitX)
        offsetY.snapTo(limitY)
    }

    companion object {
        val SAVER: Saver<MediaViewerState, *> = listSaver(save = {
            listOf(it.offsetX.value, it.offsetY.value, it.scale.value, it.rotation.value)
        }, restore = {
            val state = MediaViewerState(
                offsetX = it[0],
                offsetY = it[1],
                scale = it[2],
                rotation = it[3],
            )
            state.fromSaver = true
            state
        })
    }
}

@Composable
fun rememberViewerState(
    // X轴偏移量
    offsetX: Float = DEFAULT_OFFSET_X,
    // Y轴偏移量
    offsetY: Float = DEFAULT_OFFSET_Y,
    // 缩放率
    scale: Float = DEFAULT_SCALE,
    // 旋转
    rotation: Float = DEFAULT_ROTATION,
): MediaViewerState = rememberSaveable(saver = MediaViewerState.SAVER) {
    MediaViewerState(offsetX, offsetY, scale, rotation)
}