package com.ismartcoding.plain.ui.components.mediaviewer.hugeimage

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.ui.components.mediaviewer.DEFAULT_OFFSET_X
import com.ismartcoding.plain.ui.components.mediaviewer.DEFAULT_OFFSET_Y
import com.ismartcoding.plain.ui.components.mediaviewer.DEFAULT_ROTATION
import com.ismartcoding.plain.ui.components.mediaviewer.DEFAULT_SCALE
import com.ismartcoding.plain.ui.components.mediaviewer.RawGesture
import com.ismartcoding.plain.ui.components.mediaviewer.SizeChangeContent
import com.ismartcoding.plain.ui.components.mediaviewer.detectTransformGestures
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.DEFAULT_CROSS_FADE_ANIMATE_SPEC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode


@Composable
fun MediaHugeImage(
    modifier: Modifier = Modifier,
    imageDecoder: ImageDecoder,
    scale: Float = DEFAULT_SCALE,
    offsetX: Float = DEFAULT_OFFSET_X,
    offsetY: Float = DEFAULT_OFFSET_Y,
    rotation: Float = DEFAULT_ROTATION,
    gesture: RawGesture = RawGesture(),
    onMounted: () -> Unit = {},
    onSizeChange: suspend (SizeChangeContent) -> Unit = {},
    boundClip: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    // 容器大小
    var bSize by remember { mutableStateOf(IntSize.Zero) }
    // 容器长宽比
    val bRatio by remember { derivedStateOf { bSize.width.toFloat() / bSize.height.toFloat() } }
    // 原图长宽比
    val oRatio by remember { derivedStateOf { imageDecoder.decoderWidth.toFloat() / imageDecoder.decoderHeight.toFloat() } }
    // 是否宽度与容器大小一致
    var widthFixed by remember { mutableStateOf(false) }
    // 长宽是否均超出容器长宽
    val superSize by remember {
        derivedStateOf {
            imageDecoder.decoderHeight > bSize.height && imageDecoder.decoderWidth > bSize.width
        }
    }
    // 显示的默认大小
    val uSize by remember {
        derivedStateOf {
            if (oRatio > bRatio) {
                // 宽度一致
                val uW = bSize.width
                val uH = uW / oRatio
                widthFixed = true
                IntSize(uW, uH.toInt())
            } else {
                // 高度一致
                val uH = bSize.height
                val uW = uH * oRatio
                widthFixed = false
                IntSize(uW.toInt(), uH)
            }
        }
    }
    // 显示的实际大小
    val rSize by remember(key1 = scale) {
        derivedStateOf {
            IntSize(
                (uSize.width * scale).toInt(),
                (uSize.height * scale).toInt()
            )
        }
    }

    // 同时监听容器和实际图片大小的变化
    LaunchedEffect(key1 = bSize, key2 = rSize) {
        // 获取最大缩放率
        val maxScale = when {
            superSize -> {
                imageDecoder.decoderWidth.toFloat() / uSize.width.toFloat()
            }

            widthFixed -> {
                bSize.height.toFloat() / uSize.height.toFloat()
            }

            else -> {
                bSize.width.toFloat() / uSize.width.toFloat()
            }
        }
        // 回调
        onSizeChange(
            SizeChangeContent(
                defaultSize = uSize,
                containerSize = bSize,
                maxScale = maxScale
            )
        )
    }

    // 判断是否需要高画质渲染
    val needRenderHeightTexture by remember(key1 = bSize) {
        derivedStateOf {
            // 目前策略：原图的面积大于容器面积，就要渲染高画质
            BigDecimal(imageDecoder.decoderWidth)
                .multiply(BigDecimal(imageDecoder.decoderHeight)) > BigDecimal(bSize.height)
                .multiply(BigDecimal(bSize.width))
        }
    }
    // 标识当前是否开启高画质渲染，如果需要高画质渲染，并且缩放大于1
    val renderHeightTexture by remember(key1 = scale) { derivedStateOf { needRenderHeightTexture && scale > 1 } }
    // 当前采样率
    var inSampleSize by remember { mutableIntStateOf(1) }
    // 最小图的采样率
    var zeroInSampleSize by remember { mutableIntStateOf(8) }
    // 底图的采样率
    var backGroundInSample by remember { mutableIntStateOf(0) }
    // 底图bitmap
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    // 监听渲染实际大小，动态修改图片的采样率
    LaunchedEffect(key1 = rSize) {
        if (scale < 1F) return@LaunchedEffect
        inSampleSize = calculateInSampleSize(
            srcWidth = imageDecoder.decoderWidth,
            reqWidth = rSize.width
        )
        if (scale == 1F) {
            zeroInSampleSize = inSampleSize
        }
    }
    // 根据采样率变化，实时更新底图
    LaunchedEffect(key1 = zeroInSampleSize, key2 = inSampleSize, key3 = needRenderHeightTexture) {
        scope.launch {
            // 如果不需要渲染高画质，就不需要分块渲染，直接使用当前采样率，用底图来展示
            val iss = if (needRenderHeightTexture) zeroInSampleSize else inSampleSize
            if (iss == backGroundInSample) {
                return@launch
            }
            backGroundInSample = iss
            bitmap = withIO {
                imageDecoder.decodeRegion(
                    iss, Rect(
                        0,
                        0,
                        imageDecoder.decoderWidth,
                        imageDecoder.decoderHeight
                    )
                )
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            bitmap?.recycle()
            bitmap = null
        }
    }

    // 底图偏移量X，要确保图片在容器中居中对齐
    val deltaX by remember(key1 = offsetX, key2 = bSize, key3 = rSize) {
        derivedStateOf {
            offsetX + (bSize.width - rSize.width).toFloat().div(2)
        }
    }
    // 底图偏移量Y，要确保图片在容器中居中对齐
    val deltaY by remember(key1 = offsetY, key2 = bSize, key3 = rSize) {
        derivedStateOf {
            offsetY + (bSize.height - rSize.height).toFloat().div(2)
        }
    }
    // 计算显示区域内矩形的宽度
    val rectW by remember(key1 = offsetX) {
        derivedStateOf {
            calcLeftSize(
                bSize = bSize.width.toFloat(),
                rSize = rSize.width.toFloat(),
                offset = offsetX,
            )
        }
    }
    // 计算显示区域内矩形的高度
    val rectH by remember(key1 = offsetY, key2 = rSize) {
        derivedStateOf {
            calcLeftSize(
                bSize = bSize.height.toFloat(),
                rSize = rSize.height.toFloat(),
                offset = offsetY,
            )
        }
    }
    // 渲染可见区域的开始坐标X
    val stX by remember(key1 = offsetX) {
        derivedStateOf {
            // 计算显示区域矩形的偏移坐标
            val rectDeltaX = getRectDelta(
                deltaX,
                rSize.width.toFloat(),
                bSize.width.toFloat(),
                offsetX
            )
            // 偏移坐标减偏移量求出矩形在图片上的相对坐标
            rectDeltaX - deltaX
        }
    }
    // 渲染可见区域的开始坐标Y
    val stY by remember(key1 = offsetY) {
        derivedStateOf {
            // 计算显示区域矩形的偏移坐标
            val rectDeltaY = getRectDelta(
                deltaY,
                rSize.height.toFloat(),
                bSize.height.toFloat(),
                offsetY
            )
            // 偏移坐标减偏移量求出矩形在图片上的相对坐标
            rectDeltaY - deltaY
        }
    }
    // 开始坐标加上宽度等于结束坐标
    val edX by remember(key1 = offsetX) { derivedStateOf { stX + rectW } }
    // 开始坐标加上高度等于结束坐标
    val edY by remember(key1 = offsetY) { derivedStateOf { stY + rectH } }

    // 更新时间戳，用于通知canvas更新方块
    var renderUpdateTimeStamp by remember { mutableLongStateOf(0L) }
    // 开启解码队列的循环
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            imageDecoder.startRenderQueueAsync {
                // 解码器解码一个，就更新一次时间戳
                scope.launch {
                    renderUpdateTimeStamp = System.currentTimeMillis()
                }
            }
        }
    }
    // 切换到不需要高画质渲染时，需要清除解码队列，清除全部的bitmap
    LaunchedEffect(key1 = renderHeightTexture) {
        if (!renderHeightTexture) {
            imageDecoder.renderQueue.clear()
            imageDecoder.clearAllBitmap()
        }
    }

    /**
     * 更新渲染队列
     */
    var calcMaxCountPending by remember { mutableStateOf(false) }
    // 先前的缩放比
    var previousScale by remember { mutableStateOf<Float?>(null) }
    // 先前的偏移量
    var previousOffset by remember { mutableStateOf<Offset?>(null) }

    // 记录最长边的最大方块数
    var blockDividerCount by remember { mutableIntStateOf(1) }
    // 用来标识这个参数是否有改变
    var preBlockDividerCount by remember { mutableIntStateOf(blockDividerCount) }

    // 更新渲染方块的信息
    fun updateRenderList() {
        // 如果此时正在重新计算渲染方块的数目，就退出
        if (calcMaxCountPending) return
        // 更新的时候如果缩放和偏移量没有变化，方块数量也没变，就没有必要计算了
        if (
            previousOffset?.x == offsetX
            && previousOffset?.y == offsetY
            && previousScale == scale
            && preBlockDividerCount == blockDividerCount
        ) return
        previousScale = scale
        previousOffset = Offset(offsetX, offsetY)
        // 计算当前渲染方块大小
        val renderBlockSize =
            imageDecoder.blockSize * (rSize.width.toFloat().div(imageDecoder.decoderWidth))
        var tlx: Int
        var tly: Int
        var startX: Float
        var startY: Float
        var endX: Float
        var endY: Float
        var eh: Int
        var ew: Int
        var needUpdate: Boolean
        var previousInBound: Boolean
        var previousInSampleSize: Int
        var lastX: Int?
        var lastY: Int? = null
        var lastXDelta: Int
        var lastYDelta: Int
        val insertList = ArrayList<RenderBlock>()
        val removeList = ArrayList<RenderBlock>()
        for ((column, list) in imageDecoder.renderList.withIndex()) {
            startY = column * renderBlockSize
            endY = (column + 1) * renderBlockSize
            tly = (deltaY + startY).toInt()
            eh = (if (endY > rSize.height) rSize.height - startY else renderBlockSize).toInt()
            // 由于计算的精度问题，需要确保每一个区块都要严丝合缝
            lastY?.let {
                if (it < tly) {
                    lastYDelta = tly - it
                    tly = it
                    eh += lastYDelta
                }
            }
            lastY = tly + eh
            lastX = null
            for ((row, block) in list.withIndex()) {
                startX = row * renderBlockSize
                tlx = (deltaX + startX).toInt()
                endX = (row + 1) * renderBlockSize
                ew = (if (endX > rSize.width) rSize.width - startX else renderBlockSize).toInt()
                previousInSampleSize = block.inSampleSize
                previousInBound = block.inBound
                // 记录当前区块的采用率
                block.inSampleSize = inSampleSize
                // 判断区块是否在可视范围内
                block.inBound = checkRectInBound(
                    startX, startY, endX, endY,
                    stX, stY, edX, edY
                )
                // 由于计算的精度问题，需要确保每一个区块都要严丝合缝
                lastX?.let {
                    if (it < tlx) {
                        lastXDelta = tlx - it
                        tlx = it
                        ew += lastXDelta
                    }
                }
                lastX = tlx + ew
                // 记录区块的实际偏移量
                block.renderOffset = IntOffset(tlx, tly)
                // 记录区块的实际大小
                block.renderSize = IntSize(
                    width = ew,
                    height = eh,
                )
                // 如果参数跟之前的一样，就没有必要更新bitmap
                needUpdate = previousInBound != block.inBound
                        || previousInSampleSize != block.inSampleSize
                if (!needUpdate) continue
                if (!renderHeightTexture) continue
                // 解码队列操作时是有锁的，会对性能造成影响
                if (block.inBound) {
                    if (!imageDecoder.renderQueue.contains(block)) {
                        insertList.add(block)
                    }
                } else {
                    removeList.add(block)
                    block.release()
                }
            }
        }
        scope.launch(Dispatchers.IO) {
            synchronized(imageDecoder.renderQueue) {
                insertList.forEach {
                    imageDecoder.renderQueue.putFirst(it)
                }
                removeList.forEach {
                    imageDecoder.renderQueue.remove(it)
                }
            }
        }
    }

    LaunchedEffect(key1 = rSize, key2 = rectW, key3 = rectH) {
        // 可视区域面积
        val rectArea = BigDecimal(rectW.toDouble()).multiply(BigDecimal(rectH.toDouble()))
        // 实际大小面积
        val realArea = BigDecimal(rSize.width).multiply(BigDecimal(rSize.height))
        // 被除数不能为0
        if (realArea.toFloat() == 0F) return@LaunchedEffect
        // 计算实际面积的可视率
        val renderAreaPercentage =
            rectArea.divide(realArea, 2, RoundingMode.HALF_EVEN).toFloat()
        // 根据不同可视率，匹配合适的方块数，最大只能到8
        val goBlockDividerCount = when {
            renderAreaPercentage > 0.6F -> 1
            renderAreaPercentage > 0.025F -> 4
            else -> 8
        }
        // 如果没变，就不要修改
        if (goBlockDividerCount == blockDividerCount) return@LaunchedEffect
        preBlockDividerCount = blockDividerCount
        blockDividerCount = goBlockDividerCount
        scope.launch {
            // 清空解码队列
            imageDecoder.renderQueue.clear()
            // 进入修改区间
            calcMaxCountPending = true
            imageDecoder.setMaxBlockCount(blockDividerCount)
            calcMaxCountPending = false
            // 离开修改区间
            updateRenderList()
        }
    }

    // 旋转中心
    val rotationCenter by remember(key1 = offsetX, key2 = offsetY, key3 = scale) {
        derivedStateOf {
            val cx = deltaX + rSize.width.div(2)
            val cy = deltaY + rSize.height.div(2)
            Offset(cx, cy)
        }
    }

    /**
     * canvas加载成功后避免闪一下
     */
    val canvasAlpha = remember { Animatable(0F) }
    LaunchedEffect(key1 = bitmap) {
        if (bitmap != null && bitmap!!.width > 1 && bitmap!!.height > 1) {
            if (canvasAlpha.value == 0F) {
                scope.launch {
                    canvasAlpha.animateTo(
                        targetValue = 1F,
                        animationSpec = DEFAULT_CROSS_FADE_ANIMATE_SPEC
                    )
                    onMounted()
                }
            }
        }
    }

    Canvas(
        modifier = modifier
            .alpha(canvasAlpha.value)
            .fillMaxSize()
            .graphicsLayer {
                // 图片位移时会超出容器大小，需要在这个地方指定是否裁切
                clip = boundClip
            }
            .onSizeChanged {
                bSize = it
            }
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = gesture.onLongPress)
            }
            .pointerInput(Unit) {
                detectTransformGestures(
                    onTap = gesture.onTap,
                    onDoubleTap = gesture.onDoubleTap,
                    gestureStart = gesture.gestureStart,
                    gestureEnd = gesture.gestureEnd,
                    onGesture = gesture.onGesture,
                )
            },
    ) {
        withTransform({
            rotate(degrees = rotation, pivot = rotationCenter)
        }) {
            if (bitmap != null) {
                drawImage(
                    image = bitmap!!.asImageBitmap(),
                    dstSize = IntSize(rSize.width, rSize.height),
                    dstOffset = IntOffset(deltaX.toInt(), deltaY.toInt()),
                )
            }
            // 更新渲染队列
            if (renderUpdateTimeStamp >= 0) updateRenderList()
            if (renderHeightTexture && !calcMaxCountPending) {
                imageDecoder.forEachBlock { block, _, _ ->
                    block.bitmap?.let {
                        try {
                            drawImage(
                                image = it.asImageBitmap(),
                                dstSize = block.renderSize,
                                dstOffset = block.renderOffset
                            )
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                            LogCat.e(ex.toString())
                        }
                    }
                }
            }
        }
    }
}

