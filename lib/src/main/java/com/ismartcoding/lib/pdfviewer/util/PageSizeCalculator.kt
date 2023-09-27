package com.ismartcoding.lib.pdfviewer.util

import com.ismartcoding.lib.pdfviewer.util.MathUtils.floor
import com.shockwave.pdfium.util.Size
import com.shockwave.pdfium.util.SizeF

class PageSizeCalculator(
    private val fitPolicy: FitPolicy,
    private val originalMaxWidthPageSize: Size,
    private val originalMaxHeightPageSize: Size,
    private val viewSize: Size,
    private val fitEachPage: Boolean,
) {
    lateinit var optimalMaxWidthPageSize: SizeF
        private set
    lateinit var optimalMaxHeightPageSize: SizeF
        private set
    private var widthRatio = 0f
    private var heightRatio = 0f

    init {
        calculateMaxPages()
    }

    fun calculate(pageSize: Size): SizeF {
        if (pageSize.width <= 0 || pageSize.height <= 0) {
            return SizeF(0F, 0F)
        }
        val maxWidth = if (fitEachPage) viewSize.width.toFloat() else pageSize.width * widthRatio
        val maxHeight = if (fitEachPage) viewSize.height.toFloat() else pageSize.height * heightRatio
        return when (fitPolicy) {
            FitPolicy.HEIGHT -> fitHeight(pageSize, maxHeight)
            FitPolicy.BOTH -> fitBoth(pageSize, maxWidth, maxHeight)
            else -> fitWidth(pageSize, maxWidth)
        }
    }

    private fun calculateMaxPages() {
        when (fitPolicy) {
            FitPolicy.HEIGHT -> {
                optimalMaxHeightPageSize = fitHeight(originalMaxHeightPageSize, viewSize.height.toFloat())
                heightRatio = optimalMaxHeightPageSize.height / originalMaxHeightPageSize.height
                optimalMaxWidthPageSize = fitHeight(originalMaxWidthPageSize, originalMaxWidthPageSize.height * heightRatio)
            }
            FitPolicy.BOTH -> {
                val localOptimalMaxWidth = fitBoth(originalMaxWidthPageSize, viewSize.width.toFloat(), viewSize.height.toFloat())
                val localWidthRatio = localOptimalMaxWidth.width / originalMaxWidthPageSize.width
                optimalMaxHeightPageSize =
                    fitBoth(
                        originalMaxHeightPageSize, originalMaxHeightPageSize.width * localWidthRatio,
                        viewSize.height.toFloat(),
                    )
                heightRatio = optimalMaxHeightPageSize.height / originalMaxHeightPageSize.height
                optimalMaxWidthPageSize = fitBoth(originalMaxWidthPageSize, viewSize.width.toFloat(), originalMaxWidthPageSize.height * heightRatio)
                widthRatio = optimalMaxWidthPageSize.width / originalMaxWidthPageSize.width
            }
            else -> {
                optimalMaxWidthPageSize = fitWidth(originalMaxWidthPageSize, viewSize.width.toFloat())
                widthRatio = optimalMaxWidthPageSize.width / originalMaxWidthPageSize.width
                optimalMaxHeightPageSize = fitWidth(originalMaxHeightPageSize, originalMaxHeightPageSize.width * widthRatio)
            }
        }
    }

    private fun fitWidth(
        pageSize: Size,
        maxWidth: Float,
    ): SizeF {
        var w = pageSize.width.toFloat()
        var h = pageSize.height.toFloat()
        val ratio = w / h
        w = maxWidth
        h = floor(maxWidth / ratio).toFloat()
        return SizeF(w, h)
    }

    private fun fitHeight(
        pageSize: Size,
        maxHeight: Float,
    ): SizeF {
        var w = pageSize.width.toFloat()
        var h = pageSize.height.toFloat()
        val ratio = h / w
        h = maxHeight
        w = floor(maxHeight / ratio).toFloat()
        return SizeF(w, h)
    }

    private fun fitBoth(
        pageSize: Size,
        maxWidth: Float,
        maxHeight: Float,
    ): SizeF {
        var w = pageSize.width.toFloat()
        var h = pageSize.height.toFloat()
        val ratio = w / h
        w = maxWidth
        h = floor(maxWidth / ratio).toFloat()
        if (h > maxHeight) {
            h = maxHeight
            w = floor(maxHeight * ratio).toFloat()
        }
        return SizeF(w, h)
    }
}
