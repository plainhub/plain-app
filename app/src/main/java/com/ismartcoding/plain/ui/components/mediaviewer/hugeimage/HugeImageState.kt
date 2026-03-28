package com.ismartcoding.plain.ui.components.mediaviewer.hugeimage

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.math.BigDecimal

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
