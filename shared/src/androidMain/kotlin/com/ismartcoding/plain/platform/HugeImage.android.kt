package com.ismartcoding.plain.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Matrix
import android.graphics.Rect
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
import androidx.compose.animation.core.Animatable
import com.ismartcoding.plain.enums.RotationType
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.ui.components.mediaviewer.DEFAULT_CROSS_FADE_ANIMATE_SPEC
import com.ismartcoding.plain.ui.components.mediaviewer.DEFAULT_OFFSET_X
import com.ismartcoding.plain.ui.components.mediaviewer.DEFAULT_OFFSET_Y
import com.ismartcoding.plain.ui.components.mediaviewer.DEFAULT_ROTATION
import com.ismartcoding.plain.ui.components.mediaviewer.DEFAULT_SCALE
import com.ismartcoding.plain.ui.components.mediaviewer.RawGesture
import com.ismartcoding.plain.ui.components.mediaviewer.SizeChangeContent
import com.ismartcoding.plain.ui.components.mediaviewer.detectTransformGestures
import com.ismartcoding.plain.ui.components.mediaviewer.hugeimage.RotationIllegalException
import com.ismartcoding.plain.ui.components.mediaviewer.hugeimage.calcLeftSize
import com.ismartcoding.plain.ui.components.mediaviewer.hugeimage.calculateInSampleSize
import com.ismartcoding.plain.ui.components.mediaviewer.hugeimage.checkRectInBound
import com.ismartcoding.plain.ui.components.mediaviewer.hugeimage.getRectDelta
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.concurrent.LinkedBlockingDeque
import kotlin.math.ceil

// ---- RenderBlock (moved from ui/components/mediaviewer/hugeimage/) ----

data class RenderBlock(
    var inBound: Boolean = false,
    var inSampleSize: Int = 1,
    var renderOffset: IntOffset = IntOffset.Zero,
    var renderSize: IntSize = IntSize.Zero,
    var sliceRect: Rect = Rect(0, 0, 0, 0),
    var bitmap: Bitmap? = null,
) {
    fun release() {
        bitmap?.recycle()
        bitmap = null
    }
}

// ---- ImageDecoderRegion helpers (moved from ui/components/mediaviewer/hugeimage/) ----

internal fun buildRenderBlockList(
    countH: Int,
    countW: Int,
    blockSize: Int,
    decoderWidth: Int,
    decoderHeight: Int,
): Array<Array<RenderBlock>> {
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

internal fun getRotateBitmap(bitmap: Bitmap, degree: Float): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(degree)
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
}

/**
 * 解码渲染区域
 */
internal fun decodeRegionBitmap(
    decoder: BitmapRegionDecoder,
    rotation: Int,
    decoderWidth: Int,
    decoderHeight: Int,
    inSampleSize: Int,
    rect: Rect,
): Bitmap? {
    synchronized(decoder) {
        return try {
            val ops = BitmapFactory.Options()
            ops.inSampleSize = inSampleSize
            if (decoder.isRecycled) return null
            return if (rotation == RotationType.ROTATION_0.value) {
                decoder.decodeRegion(rect, ops)
            } else {
                val newRect = when (rotation) {
                    RotationType.ROTATION_90.value -> {
                        val nextX1 = rect.top
                        val nextX2 = rect.bottom
                        val nextY1 = decoderWidth - rect.right
                        val nextY2 = decoderWidth - rect.left
                        Rect(nextX1, nextY1, nextX2, nextY2)
                    }

                    RotationType.ROTATION_180.value -> {
                        val nextX1 = decoderWidth - rect.right
                        val nextX2 = decoderWidth - rect.left
                        val nextY1 = decoderHeight - rect.bottom
                        val nextY2 = decoderHeight - rect.top
                        Rect(nextX1, nextY1, nextX2, nextY2)
                    }

                    RotationType.ROTATION_270.value -> {
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

// ---- ImageDecoder (moved from ui/components/mediaviewer/hugeimage/) ----

class ImageDecoder(
    private val decoder: BitmapRegionDecoder,
    private val rotation: Int = RotationType.ROTATION_0.value,
    private val onRelease: () -> Unit = {},
) {
    var decoderWidth by mutableIntStateOf(0)
        private set

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

    // 设置最长边最大方块数
    @Synchronized
    fun setMaxBlockCount(count: Int): Boolean {
        if (maxBlockCount == count) return false
        if (decoder.isRecycled) return false

        when (rotation) {
            RotationType.ROTATION_0.value, RotationType.ROTATION_180.value -> {
                decoderWidth = decoder.width
                decoderHeight = decoder.height
            }

            RotationType.ROTATION_90.value, RotationType.ROTATION_270.value -> {
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
        renderList = buildRenderBlockList(countH, countW, blockSize, decoderWidth, decoderHeight)
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

    /**
     * 解码渲染区域
     */
    fun decodeRegion(inSampleSize: Int, rect: Rect) =
        decodeRegionBitmap(decoder, rotation, decoderWidth, decoderHeight, inSampleSize, rect)

    fun startRenderQueueAsync(onUpdate: () -> Unit) {
        try {
            while (!decoder.isRecycled) {
                val block = renderQueue.take()
                if (decoder.isRecycled) {
                    break
                }
                val bitmap = decodeRegion(block.inSampleSize, block.sliceRect)
                if (bitmap != null) {
                    block.bitmap = bitmap
                }
                onUpdate()
            }
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
}

// ---- HugeImageState (moved from ui/components/mediaviewer/hugeimage/) ----

class HugeImageState(
    val imageDecoder: ImageDecoder,
    val scope: CoroutineScope,
) {
    var scale by mutableStateOf(1f)
    var offsetX by mutableStateOf(0f)
    var offsetY by mutableStateOf(0f)
    var bSize by mutableStateOf(IntSize.Zero)
    var widthFixed by mutableStateOf(false)
    var inSampleSize by mutableIntStateOf(1)
    var zeroInSampleSize by mutableIntStateOf(8)
    var backGroundInSample by mutableIntStateOf(0)
    var bitmap by mutableStateOf<Bitmap?>(null)
    var renderUpdateTimeStamp by mutableLongStateOf(0L)
    var calcMaxCountPending by mutableStateOf(false)
    var previousScale by mutableStateOf<Float?>(null)
    var previousOffset by mutableStateOf<Offset?>(null)
    var blockDividerCount by mutableIntStateOf(1)
    var preBlockDividerCount by mutableIntStateOf(1)
    val canvasAlpha = Animatable(0F)

    val bRatio by derivedStateOf { if (bSize.height == 0) 1f else bSize.width.toFloat() / bSize.height.toFloat() }
    val oRatio by derivedStateOf { imageDecoder.decoderWidth.toFloat() / imageDecoder.decoderHeight.toFloat() }
    val superSize by derivedStateOf { imageDecoder.decoderHeight > bSize.height && imageDecoder.decoderWidth > bSize.width }

    val uSize by derivedStateOf {
        if (oRatio > bRatio) {
            val uW = bSize.width; val uH = uW / oRatio; widthFixed = true; IntSize(uW, uH.toInt())
        } else {
            val uH = bSize.height; val uW = uH * oRatio; widthFixed = false; IntSize(uW.toInt(), uH)
        }
    }

    val rSize by derivedStateOf { IntSize((uSize.width * scale).toInt(), (uSize.height * scale).toInt()) }

    val needRenderHeightTexture by derivedStateOf {
        BigDecimal(imageDecoder.decoderWidth).multiply(BigDecimal(imageDecoder.decoderHeight)) >
                BigDecimal(bSize.height).multiply(BigDecimal(bSize.width))
    }

    val renderHeightTexture by derivedStateOf { needRenderHeightTexture && scale > 1 }

    val deltaX by derivedStateOf { offsetX + (bSize.width - rSize.width).toFloat().div(2) }
    val deltaY by derivedStateOf { offsetY + (bSize.height - rSize.height).toFloat().div(2) }

    val rectW by derivedStateOf { calcLeftSize(bSize.width.toFloat(), rSize.width.toFloat(), offsetX) }
    val rectH by derivedStateOf { calcLeftSize(bSize.height.toFloat(), rSize.height.toFloat(), offsetY) }

    val stX by derivedStateOf { getRectDelta(deltaX, rSize.width.toFloat(), bSize.width.toFloat(), offsetX) - deltaX }
    val stY by derivedStateOf { getRectDelta(deltaY, rSize.height.toFloat(), bSize.height.toFloat(), offsetY) - deltaY }
    val edX by derivedStateOf { stX + rectW }
    val edY by derivedStateOf { stY + rectH }

    val rotationCenter by derivedStateOf { Offset(deltaX + rSize.width.div(2), deltaY + rSize.height.div(2)) }

    val maxScale: Float
        get() = when {
            superSize -> imageDecoder.decoderWidth.toFloat() / uSize.width.toFloat()
            widthFixed -> bSize.height.toFloat() / uSize.height.toFloat()
            else -> bSize.width.toFloat() / uSize.width.toFloat()
        }

    fun updateSampleSize() {
        if (scale < 1F) return
        inSampleSize = calculateInSampleSize(srcWidth = imageDecoder.decoderWidth, reqWidth = rSize.width)
        if (scale == 1F) zeroInSampleSize = inSampleSize
    }

    fun updateBackgroundBitmap() {
        scope.launch {
            val iss = if (needRenderHeightTexture) zeroInSampleSize else inSampleSize
            if (iss == backGroundInSample) return@launch
            backGroundInSample = iss
            bitmap = withIO {
                imageDecoder.decodeRegion(iss, Rect(0, 0, imageDecoder.decoderWidth, imageDecoder.decoderHeight))
            }
        }
    }
}

// ---- HugeImageRenderHelper (moved from ui/components/mediaviewer/hugeimage/) ----

fun HugeImageState.updateRenderList() {
    if (calcMaxCountPending) return
    if (previousOffset?.x == offsetX && previousOffset?.y == offsetY
        && previousScale == scale && preBlockDividerCount == blockDividerCount
    ) return
    previousScale = scale
    previousOffset = Offset(offsetX, offsetY)

    val renderBlockSize = imageDecoder.blockSize * (rSize.width.toFloat().div(imageDecoder.decoderWidth))
    var tlx: Int; var tly: Int; var startX: Float; var startY: Float
    var endX: Float; var endY: Float; var eh: Int; var ew: Int
    var needUpdate: Boolean; var previousInBound: Boolean; var previousInSampleSize: Int
    var lastX: Int?; var lastY: Int? = null; var lastXDelta: Int; var lastYDelta: Int
    val insertList = ArrayList<RenderBlock>()
    val removeList = ArrayList<RenderBlock>()

    for ((column, list) in imageDecoder.renderList.withIndex()) {
        startY = column * renderBlockSize
        endY = (column + 1) * renderBlockSize
        tly = (deltaY + startY).toInt()
        eh = (if (endY > rSize.height) rSize.height - startY else renderBlockSize).toInt()
        lastY?.let { if (it < tly) { lastYDelta = tly - it; tly = it; eh += lastYDelta } }
        lastY = tly + eh
        lastX = null
        for ((row, block) in list.withIndex()) {
            startX = row * renderBlockSize
            tlx = (deltaX + startX).toInt()
            endX = (row + 1) * renderBlockSize
            ew = (if (endX > rSize.width) rSize.width - startX else renderBlockSize).toInt()
            previousInSampleSize = block.inSampleSize
            previousInBound = block.inBound
            block.inSampleSize = inSampleSize
            block.inBound = checkRectInBound(startX, startY, endX, endY, stX, stY, edX, edY)
            lastX?.let { if (it < tlx) { lastXDelta = tlx - it; tlx = it; ew += lastXDelta } }
            lastX = tlx + ew
            block.renderOffset = IntOffset(tlx, tly)
            block.renderSize = IntSize(width = ew, height = eh)
            needUpdate = previousInBound != block.inBound || previousInSampleSize != block.inSampleSize
            if (!needUpdate) continue
            if (!renderHeightTexture) continue
            if (block.inBound) {
                if (!imageDecoder.renderQueue.contains(block)) insertList.add(block)
            } else {
                removeList.add(block); block.release()
            }
        }
    }
    scope.launch(Dispatchers.Default) {
        synchronized(imageDecoder.renderQueue) {
            insertList.forEach { imageDecoder.renderQueue.putFirst(it) }
            removeList.forEach { imageDecoder.renderQueue.remove(it) }
        }
    }
}

fun HugeImageState.updateBlockDivider(rectW: Float, rectH: Float) {
    val rectArea = java.math.BigDecimal(rectW.toDouble()).multiply(java.math.BigDecimal(rectH.toDouble()))
    val realArea = java.math.BigDecimal(rSize.width).multiply(java.math.BigDecimal(rSize.height))
    if (realArea.toFloat() == 0F) return
    val renderAreaPercentage = rectArea.divide(realArea, 2, java.math.RoundingMode.HALF_EVEN).toFloat()
    val goBlockDividerCount = when {
        renderAreaPercentage > 0.6F -> 1
        renderAreaPercentage > 0.025F -> 4
        else -> 8
    }
    if (goBlockDividerCount == blockDividerCount) return
    preBlockDividerCount = blockDividerCount
    blockDividerCount = goBlockDividerCount
    scope.launch {
        imageDecoder.renderQueue.clear()
        calcMaxCountPending = true
        imageDecoder.setMaxBlockCount(blockDividerCount)
        calcMaxCountPending = false
        updateRenderList()
    }
}

// ---- MediaHugeImage composable (moved from ui/components/mediaviewer/hugeimage/) ----

@Composable
fun MediaHugeImage(
    modifier: Modifier = Modifier, imageDecoder: ImageDecoder,
    scale: Float = DEFAULT_SCALE, offsetX: Float = DEFAULT_OFFSET_X,
    offsetY: Float = DEFAULT_OFFSET_Y, rotation: Float = DEFAULT_ROTATION,
    gesture: RawGesture = RawGesture(), onMounted: () -> Unit = {},
    onSizeChange: suspend (SizeChangeContent) -> Unit = {}, boundClip: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    val state = remember { HugeImageState(imageDecoder, scope) }
    state.scale = scale; state.offsetX = offsetX; state.offsetY = offsetY

    LaunchedEffect(state.bSize, state.rSize) {
        onSizeChange(SizeChangeContent(defaultSize = state.uSize, containerSize = state.bSize, maxScale = state.maxScale))
    }
    LaunchedEffect(state.rSize) { state.updateSampleSize() }
    LaunchedEffect(state.zeroInSampleSize, state.inSampleSize, state.needRenderHeightTexture) { state.updateBackgroundBitmap() }
    DisposableEffect(Unit) { onDispose { state.bitmap?.recycle(); state.bitmap = null } }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.Default) {
            imageDecoder.startRenderQueueAsync { scope.launch { state.renderUpdateTimeStamp = System.currentTimeMillis() } }
        }
    }
    LaunchedEffect(state.renderHeightTexture) {
        if (!state.renderHeightTexture) { imageDecoder.renderQueue.clear(); imageDecoder.clearAllBitmap() }
    }
    LaunchedEffect(state.rSize, state.rectW, state.rectH) { state.updateBlockDivider(state.rectW, state.rectH) }
    LaunchedEffect(state.bitmap) {
        val bmp = state.bitmap
        if (bmp != null && bmp.width > 1 && bmp.height > 1 && state.canvasAlpha.value == 0F) {
            scope.launch { state.canvasAlpha.animateTo(1F, DEFAULT_CROSS_FADE_ANIMATE_SPEC); onMounted() }
        }
    }

    Canvas(
        modifier = modifier.alpha(state.canvasAlpha.value).fillMaxSize()
            .graphicsLayer { clip = boundClip }
            .onSizeChanged { state.bSize = it }
            .pointerInput(Unit) { detectTapGestures(onLongPress = gesture.onLongPress) }
            .pointerInput(Unit) {
                detectTransformGestures(onTap = gesture.onTap, onDoubleTap = gesture.onDoubleTap,
                    gestureStart = gesture.gestureStart, gestureEnd = gesture.gestureEnd, onGesture = gesture.onGesture)
            },
    ) {
        withTransform({ rotate(degrees = rotation, pivot = state.rotationCenter) }) {
            state.bitmap?.let {
                drawImage(image = it.asImageBitmap(), dstSize = IntSize(state.rSize.width, state.rSize.height),
                    dstOffset = IntOffset(state.deltaX.toInt(), state.deltaY.toInt()))
            }
            if (state.renderUpdateTimeStamp >= 0) state.updateRenderList()
            if (state.renderHeightTexture && !state.calcMaxCountPending) {
                imageDecoder.forEachBlock { block, _, _ ->
                    block.bitmap?.let {
                        try { drawImage(image = it.asImageBitmap(), dstSize = block.renderSize, dstOffset = block.renderOffset) }
                        catch (ex: Exception) { ex.printStackTrace(); LogCat.e(ex.toString()) }
                    }
                }
            }
        }
    }
}
