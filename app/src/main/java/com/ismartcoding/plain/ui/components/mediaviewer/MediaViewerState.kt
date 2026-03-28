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
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow

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