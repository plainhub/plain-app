package com.ismartcoding.plain.ui.components.mediaviewer.hugeimage

import android.graphics.BitmapRegionDecoder
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