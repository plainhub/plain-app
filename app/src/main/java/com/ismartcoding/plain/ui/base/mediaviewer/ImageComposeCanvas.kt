package com.ismartcoding.plain.ui.base.mediaviewer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Matrix
import android.graphics.Rect
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.ismartcoding.plain.ui.base.mediaviewer.previewer.DEFAULT_CROSS_FADE_ANIMATE_SPEC
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.LinkedBlockingDeque
import kotlin.math.absoluteValue
import kotlin.math.ceil

data class RenderBlock(
    var inBound: Boolean = false,
    var inSampleSize: Int = 1,
    var renderOffset: IntOffset = IntOffset.Zero,
    var renderSize: IntSize = IntSize.Zero,
    var sliceRect: Rect = Rect(0, 0, 0, 0),
    private var bitmap: Bitmap? = null,
) {

    fun release() {
        bitmap?.recycle()
        bitmap = null
    }

    fun getBitmap(): Bitmap? {
        return bitmap
    }

    fun setBitmap(bitmap: Bitmap) {
        this.bitmap = bitmap
    }

}

val ROTATION_0 = 0
val ROTATION_90 = 90
val ROTATION_180 = 180
val ROTATION_270 = 270

class RotationIllegalException(msg: String = "Illegal rotation angle."): RuntimeException(msg)

class ImageDecoder(
    private val decoder: BitmapRegionDecoder,
    private val rotation: Int = ROTATION_0,
    private val onRelease: () -> Unit = {},
) : CoroutineScope by MainScope() {

    // 解码的宽度
    var decoderWidth by mutableIntStateOf(0)
        private set

    // 解码的高度
    var decoderHeight by mutableIntStateOf(0)
        private set

    // 解码区块大小
    var blockSize by mutableIntStateOf(0)
        private set

    // 渲染列表
    var renderList: Array<Array<RenderBlock>> = emptyArray()
        private set

    // 解码渲染队列
    val renderQueue = LinkedBlockingDeque<RenderBlock>()

    // 横向方块数
    private var countW = 0

    // 纵向方块数
    private var countH = 0

    // 最长边的最大方块数
    private var maxBlockCount = 0

    init {
        // 初始化最大方块数
        setMaxBlockCount(1)
    }

    // 构造一个渲染方块队列
    private fun getRenderBlockList(): Array<Array<RenderBlock>> {
        var endX: Int
        var endY: Int
        var sliceStartX: Int
        var sliceStartY: Int
        var sliceEndX: Int
        var sliceEndY: Int
        return Array(countH) { column ->
            sliceStartY = (column * blockSize)
            endY = (column + 1) * blockSize
            sliceEndY = if (endY > decoderHeight) decoderHeight else endY
            Array(countW) { row ->
                sliceStartX = (row * blockSize)
                endX = (row + 1) * blockSize
                sliceEndX = if (endX > decoderWidth) decoderWidth else endX
                RenderBlock(
                    sliceRect = Rect(
                        sliceStartX,
                        sliceStartY,
                        sliceEndX,
                        sliceEndY,
                    )
                )
            }
        }
    }

    // 设置最长边最大方块数
    fun setMaxBlockCount(count: Int): Boolean {
        if (maxBlockCount == count) return false
        if (decoder.isRecycled) return false

        when (rotation) {
            ROTATION_0, ROTATION_180 -> {
                decoderWidth = decoder.width
                decoderHeight = decoder.height
            }
            ROTATION_90, ROTATION_270 -> {
                decoderWidth = decoder.height
                decoderHeight = decoder.width
            }
            else -> throw RotationIllegalException()
        }

        maxBlockCount = count
        blockSize =
            (decoderWidth.coerceAtLeast(decoderHeight)).toFloat().div(count).toInt()
        countW = ceil(decoderWidth.toFloat().div(blockSize)).toInt()
        countH = ceil(decoderHeight.toFloat().div(blockSize)).toInt()
        renderList = getRenderBlockList()
        return true
    }

    // 遍历每一个渲染方块
    fun forEachBlock(action: (block: RenderBlock, column: Int, row: Int) -> Unit) {
        for ((column, rows) in renderList.withIndex()) {
            for ((row, block) in rows.withIndex()) {
                action(block, column, row)
            }
        }
    }

    // 清除全部bitmap的引用
    fun clearAllBitmap() {
        forEachBlock { block, _, _ ->
            block.release()
        }
    }

    // 释放资源
    fun release() {
        synchronized(decoder) {
            if (!decoder.isRecycled) {
                // 清除渲染队列
                renderQueue.clear()
                // 回收资源
                decoder.recycle()
                // 发送一个信号停止堵塞的循环
                renderQueue.putFirst(RenderBlock())
            }
            onRelease()
        }
    }

    private fun getRotateBitmap(bitmap: Bitmap, degree: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
    }

    /**
     * 解码渲染区域
     */
    fun decodeRegion(inSampleSize: Int, rect: Rect): Bitmap? {
        synchronized(decoder) {
            return try {
                val ops = BitmapFactory.Options()
                ops.inSampleSize = inSampleSize
                if (decoder.isRecycled) return null
                return if (rotation == ROTATION_0) {
                    decoder.decodeRegion(rect, ops)
                } else {
                    val newRect = when (rotation) {
                        ROTATION_90 -> {
                            val nextX1 = rect.top
                            val nextX2 = rect.bottom
                            val nextY1 = decoderWidth - rect.right
                            val nextY2 = decoderWidth - rect.left
                            Rect(nextX1, nextY1, nextX2, nextY2)
                        }
                        ROTATION_180 -> {
                            val nextX1 = decoderWidth - rect.right
                            val nextX2 = decoderWidth - rect.left
                            val nextY1 = decoderHeight - rect.bottom
                            val nextY2 = decoderHeight - rect.top
                            Rect(nextX1, nextY1, nextX2, nextY2)
                        }
                        ROTATION_270 -> {
                            val nextX1 = decoderHeight - rect.bottom
                            val nextX2 = decoderHeight - rect.top
                            val nextY1 = rect.left
                            val nextY2 = rect.right
                            Rect(nextX1, nextY1, nextX2, nextY2)
                        }
                        else -> throw RotationIllegalException()
                    }
                    val srcBitmap = decoder.decodeRegion(newRect, ops)
                    getRotateBitmap(bitmap = srcBitmap, rotation.toFloat())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // 开启堵塞队列的循环
    fun startRenderQueue(onUpdate: () -> Unit) {
        launch(Dispatchers.IO) {
            try {
                while (!decoder.isRecycled) {
                    val block = renderQueue.take()
                    if (decoder.isRecycled) break
                    val bitmap = decodeRegion(block.inSampleSize, block.sliceRect)
                    if (bitmap != null) block.setBitmap(bitmap)
                    onUpdate()
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }
}

@Composable
fun ImageComposeCanvas(
    modifier: Modifier = Modifier,
    imageDecoder: ImageDecoder,
    scale: Float = DEFAULT_SCALE,
    offsetX: Float = DEFAULT_OFFSET_X,
    offsetY: Float = DEFAULT_OFFSET_Y,
    rotation: Float = DEFAULT_ROTATION,
    gesture: RawGesture = RawGesture(),
    onMounted: () -> Unit = {},
    onSizeChange: suspend (SizeChangeContent) -> Unit = {},
    crossfadeAnimationSpec: AnimationSpec<Float> = DEFAULT_CROSS_FADE_ANIMATE_SPEC,
    boundClip: Boolean = true,
    debugMode: Boolean = false,
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
        scope.launch(Dispatchers.IO) {
            // 如果不需要渲染高画质，就不需要分块渲染，直接使用当前采样率，用底图来展示
            val iss = if (needRenderHeightTexture) zeroInSampleSize else inSampleSize
            if (iss == backGroundInSample) return@launch
            backGroundInSample = iss
            bitmap = imageDecoder.decodeRegion(
                iss, Rect(
                    0,
                    0,
                    imageDecoder.decoderWidth,
                    imageDecoder.decoderHeight
                )
            )
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
    var renderUpdateTimeStamp by remember { mutableStateOf(0L) }
    // 开启解码队列的循环
    LaunchedEffect(key1 = Unit) {
        imageDecoder.startRenderQueue {
            // 解码器解码一个，就更新一次时间戳
            renderUpdateTimeStamp = System.currentTimeMillis()
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
    var blockDividerCount by remember { mutableStateOf(1) }
    // 用来标识这个参数是否有改变
    var preBlockDividerCount by remember { mutableStateOf(blockDividerCount) }

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
        scope.launch(Dispatchers.IO) {
            // 清空解码队列
            imageDecoder.renderQueue.clear()
            // 进入修改区间
            calcMaxCountPending = true
            imageDecoder.setMaxBlockCount(blockDividerCount)
            calcMaxCountPending = false
            // 离开修改区间

            // 更新一下界面
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
                        animationSpec = crossfadeAnimationSpec
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
                    block.getBitmap()?.let {
                        drawImage(
                            image = it.asImageBitmap(),
                            dstSize = block.renderSize,
                            dstOffset = block.renderOffset
                        )
                    }
                }
            }
            // 这里会把可视区域的矩形画出来
            if (debugMode) {
                drawRect(
                    color = Color.Blue.copy(0.1F),
                    topLeft = Offset(deltaX + stX, deltaY + stY),
                    size = Size(rectW, rectH)
                )
            }
        }
    }
}

fun checkRectInBound(
    stX1: Float, stY1: Float, edX1: Float, edY1: Float,
    stX2: Float, stY2: Float, edX2: Float, edY2: Float,
): Boolean {
    if (edY1 < stY2) return false
    if (stY1 > edY2) return false
    if (edX1 < stX2) return false
    if (stX1 > edX2) return false
    return true
}

fun getRectDelta(delta: Float, rSize: Float, bSize: Float, offset: Float): Float {
    return delta + if (delta < 0) {
        val direction = if (rSize > bSize) -1 else 1
        (offset + (direction) * (bSize - rSize)
            .div(2).absoluteValue).absoluteValue
    } else 0F
}

fun calcLeftSize(bSize: Float, rSize: Float, offset: Float): Float {
    return if (offset.absoluteValue > (bSize - rSize).div(2).absoluteValue) {
        rSize - (offset.absoluteValue - (bSize - rSize).div(2))
    } else {
        rSize.coerceAtMost(bSize)
    }
}

fun calculateInSampleSize(
    srcWidth: Int,
    reqWidth: Int,
): Int {
    var inSampleSize = 1
    while (true) {
        val iss = inSampleSize * 2
        if (srcWidth.toFloat().div(iss) < reqWidth) break
        inSampleSize = iss
    }
    return inSampleSize
}