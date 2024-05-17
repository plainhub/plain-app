package com.ismartcoding.plain.ui.components.mediaviewer.hugeimage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Matrix
import android.graphics.Rect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.ismartcoding.plain.enums.RotationType
import java.util.concurrent.LinkedBlockingDeque
import kotlin.math.ceil

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