package com.ismartcoding.plain.ui.components.mediaviewer.hugeimage

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    scope.launch(Dispatchers.IO) {
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
