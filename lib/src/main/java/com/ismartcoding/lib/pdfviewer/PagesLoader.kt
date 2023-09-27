package com.ismartcoding.lib.pdfviewer

import android.graphics.RectF
import com.ismartcoding.lib.extensions.dp2px
import com.ismartcoding.lib.pdfviewer.util.Constants
import com.ismartcoding.lib.pdfviewer.util.Constants.CACHE_SIZE
import com.ismartcoding.lib.pdfviewer.util.Constants.PRELOAD_OFFSET
import com.ismartcoding.lib.pdfviewer.util.MathUtils.ceil
import com.ismartcoding.lib.pdfviewer.util.MathUtils.floor
import com.ismartcoding.lib.pdfviewer.util.MathUtils.max
import com.ismartcoding.lib.pdfviewer.util.MathUtils.min
import java.util.*

internal class PagesLoader(private val pdfView: PDFView) {
    private var cacheOrder = 0
    private var xOffset = 0f
    private var yOffset = 0f
    private var pageRelativePartWidth = 0f
    private var pageRelativePartHeight = 0f
    private var partRenderWidth = 0f
    private var partRenderHeight = 0f
    private val thumbnailRect = RectF(0F, 0F, 1F, 1F)
    private val preloadOffset = pdfView.context.dp2px(PRELOAD_OFFSET)

    private inner class Holder {
        var row = 0
        var col = 0

        override fun toString(): String {
            return "Holder{" +
                "row=" + row +
                ", col=" + col +
                '}'
        }
    }

    private inner class RenderRange internal constructor() {
        var page = 0
        val gridSize = GridSize()
        val leftTop = Holder()
        val rightBottom = Holder()

        override fun toString(): String {
            return "RenderRange{" +
                "page=" + page +
                ", gridSize=" + gridSize +
                ", leftTop=" + leftTop +
                ", rightBottom=" + rightBottom +
                '}'
        }
    }

    private inner class GridSize {
        var rows = 0
        var cols = 0

        override fun toString(): String {
            return "GridSize{" +
                "rows=" + rows +
                ", cols=" + cols +
                '}'
        }
    }

    private fun getPageColsRows(
        grid: GridSize,
        pageIndex: Int,
    ) {
        val size = pdfView.pdfFile!!.getPageSize(pageIndex)
        val ratioX = 1f / size.width
        val ratioY = 1f / size.height
        val partHeight = Constants.PART_SIZE * ratioY / pdfView.zoom
        val partWidth = Constants.PART_SIZE * ratioX / pdfView.zoom
        grid.rows = ceil(1f / partHeight)
        grid.cols = ceil(1f / partWidth)
    }

    private fun calculatePartSize(grid: GridSize) {
        pageRelativePartWidth = 1f / grid.cols.toFloat()
        pageRelativePartHeight = 1f / grid.rows.toFloat()
        partRenderWidth = Constants.PART_SIZE / pageRelativePartWidth
        partRenderHeight = Constants.PART_SIZE / pageRelativePartHeight
    }

    /**
     * calculate the render range of each page
     */
    private fun getRenderRangeList(
        firstXOffset: Float,
        firstYOffset: Float,
        lastXOffset: Float,
        lastYOffset: Float,
    ): MutableList<RenderRange> {
        val fixedFirstXOffset: Float = -max(firstXOffset, 0F)
        val fixedFirstYOffset: Float = -max(firstYOffset, 0F)
        val fixedLastXOffset: Float = -max(lastXOffset, 0F)
        val fixedLastYOffset: Float = -max(lastYOffset, 0F)
        val offsetFirst = if (pdfView.isSwipeVertical) fixedFirstYOffset else fixedFirstXOffset
        val offsetLast = if (pdfView.isSwipeVertical) fixedLastYOffset else fixedLastXOffset
        val firstPage = pdfView.pdfFile!!.getPageAtOffset(offsetFirst, pdfView.zoom)
        val lastPage = pdfView.pdfFile!!.getPageAtOffset(offsetLast, pdfView.zoom)
        val pageCount = lastPage - firstPage + 1
        val renderRanges: MutableList<RenderRange> = LinkedList()
        for (page in firstPage..lastPage) {
            val range = RenderRange()
            range.page = page
            var pageFirstXOffset: Float
            var pageFirstYOffset: Float
            var pageLastXOffset: Float
            var pageLastYOffset: Float
            if (page == firstPage) {
                pageFirstXOffset = fixedFirstXOffset
                pageFirstYOffset = fixedFirstYOffset
                if (pageCount == 1) {
                    pageLastXOffset = fixedLastXOffset
                    pageLastYOffset = fixedLastYOffset
                } else {
                    val pageOffset = pdfView.pdfFile!!.getPageOffset(page, pdfView.zoom)
                    val pageSize = pdfView.pdfFile!!.getScaledPageSize(page, pdfView.zoom)
                    if (pdfView.isSwipeVertical) {
                        pageLastXOffset = fixedLastXOffset
                        pageLastYOffset = pageOffset + pageSize.height
                    } else {
                        pageLastYOffset = fixedLastYOffset
                        pageLastXOffset = pageOffset + pageSize.width
                    }
                }
            } else if (page == lastPage) {
                val pageOffset = pdfView.pdfFile!!.getPageOffset(page, pdfView.zoom)
                if (pdfView.isSwipeVertical) {
                    pageFirstXOffset = fixedFirstXOffset
                    pageFirstYOffset = pageOffset
                } else {
                    pageFirstYOffset = fixedFirstYOffset
                    pageFirstXOffset = pageOffset
                }
                pageLastXOffset = fixedLastXOffset
                pageLastYOffset = fixedLastYOffset
            } else {
                val pageOffset = pdfView.pdfFile!!.getPageOffset(page, pdfView.zoom)
                val pageSize = pdfView.pdfFile!!.getScaledPageSize(page, pdfView.zoom)
                if (pdfView.isSwipeVertical) {
                    pageFirstXOffset = fixedFirstXOffset
                    pageFirstYOffset = pageOffset
                    pageLastXOffset = fixedLastXOffset
                    pageLastYOffset = pageOffset + pageSize.height
                } else {
                    pageFirstXOffset = pageOffset
                    pageFirstYOffset = fixedFirstYOffset
                    pageLastXOffset = pageOffset + pageSize.width
                    pageLastYOffset = fixedLastYOffset
                }
            }
            getPageColsRows(range.gridSize, range.page) // get the page's grid size that rows and cols
            val scaledPageSize = pdfView.pdfFile!!.getScaledPageSize(range.page, pdfView.zoom)
            val rowHeight = scaledPageSize.height / range.gridSize.rows
            val colWidth = scaledPageSize.width / range.gridSize.cols

            // get the page offset int the whole file
            // ---------------------------------------
            // |            |           |            |
            // |<--offset-->|   (page)  |<--offset-->|
            // |            |           |            |
            // |            |           |            |
            // ---------------------------------------
            val secondaryOffset = pdfView.pdfFile!!.getSecondaryPageOffset(page, pdfView.zoom)

            // calculate the row,col of the point in the leftTop and rightBottom
            if (pdfView.isSwipeVertical) {
                range.leftTop.row = floor(Math.abs(pageFirstYOffset - pdfView.pdfFile!!.getPageOffset(range.page, pdfView.zoom)) / rowHeight)
                range.leftTop.col = floor(min(pageFirstXOffset - secondaryOffset, 0F) / colWidth)
                range.rightBottom.row = ceil(Math.abs(pageLastYOffset - pdfView.pdfFile!!.getPageOffset(range.page, pdfView.zoom)) / rowHeight)
                range.rightBottom.col = floor(min(pageLastXOffset - secondaryOffset, 0F) / colWidth)
            } else {
                range.leftTop.col = floor(Math.abs(pageFirstXOffset - pdfView.pdfFile!!.getPageOffset(range.page, pdfView.zoom)) / colWidth)
                range.leftTop.row = floor(min(pageFirstYOffset - secondaryOffset, 0F) / rowHeight)
                range.rightBottom.col = floor(Math.abs(pageLastXOffset - pdfView.pdfFile!!.getPageOffset(range.page, pdfView.zoom)) / colWidth)
                range.rightBottom.row = floor(min(pageLastYOffset - secondaryOffset, 0F) / rowHeight)
            }
            renderRanges.add(range)
        }
        return renderRanges
    }

    private fun loadVisible() {
        var parts = 0
        val scaledPreloadOffset = preloadOffset.toFloat()
        val firstXOffset = -xOffset + scaledPreloadOffset
        val lastXOffset = -xOffset - pdfView.width - scaledPreloadOffset
        val firstYOffset = -yOffset + scaledPreloadOffset
        val lastYOffset = -yOffset - pdfView.height - scaledPreloadOffset
        val rangeList = getRenderRangeList(firstXOffset, firstYOffset, lastXOffset, lastYOffset)
        for (range in rangeList) {
            loadThumbnail(range.page)
        }
        for (range in rangeList) {
            calculatePartSize(range.gridSize)
            parts += loadPage(range.page, range.leftTop.row, range.rightBottom.row, range.leftTop.col, range.rightBottom.col, CACHE_SIZE - parts)
            if (parts >= CACHE_SIZE) {
                break
            }
        }
    }

    private fun loadPage(
        page: Int,
        firstRow: Int,
        lastRow: Int,
        firstCol: Int,
        lastCol: Int,
        nbOfPartsLoadable: Int,
    ): Int {
        var loaded = 0
        for (row in firstRow..lastRow) {
            for (col in firstCol..lastCol) {
                if (loadCell(page, row, col, pageRelativePartWidth, pageRelativePartHeight)) {
                    loaded++
                }
                if (loaded >= nbOfPartsLoadable) {
                    return loaded
                }
            }
        }
        return loaded
    }

    private fun loadCell(
        page: Int,
        row: Int,
        col: Int,
        pageRelativePartWidth: Float,
        pageRelativePartHeight: Float,
    ): Boolean {
        val relX = pageRelativePartWidth * col
        val relY = pageRelativePartHeight * row
        var relWidth = pageRelativePartWidth
        var relHeight = pageRelativePartHeight
        var renderWidth = partRenderWidth
        var renderHeight = partRenderHeight
        if (relX + relWidth > 1) {
            relWidth = 1 - relX
        }
        if (relY + relHeight > 1) {
            relHeight = 1 - relY
        }
        renderWidth *= relWidth
        renderHeight *= relHeight
        val pageRelativeBounds = RectF(relX, relY, relX + relWidth, relY + relHeight)
        if (renderWidth > 0 && renderHeight > 0) {
            if (!pdfView.cacheManager.upPartIfContained(page, pageRelativeBounds, cacheOrder)) {
                pdfView.renderingHandler!!.addRenderingTask(
                    page,
                    renderWidth,
                    renderHeight,
                    pageRelativeBounds,
                    false,
                    cacheOrder,
                    pdfView.isBestQuality,
                    pdfView.isAnnotationRendering,
                )
            }
            cacheOrder++
            return true
        }
        return false
    }

    private fun loadThumbnail(page: Int) {
        val pageSize = pdfView.pdfFile!!.getPageSize(page)
        val thumbnailWidth: Float = pageSize.width * Constants.THUMBNAIL_RATIO
        val thumbnailHeight: Float = pageSize.height * Constants.THUMBNAIL_RATIO
        if (!pdfView.cacheManager.containsThumbnail(page, thumbnailRect)) {
            pdfView.renderingHandler!!.addRenderingTask(
                page,
                thumbnailWidth,
                thumbnailHeight,
                thumbnailRect,
                true,
                0,
                pdfView.isBestQuality,
                pdfView.isAnnotationRendering,
            )
        }
    }

    fun loadPages() {
        cacheOrder = 1
        xOffset = -max(pdfView.currentXOffset, 0F)
        yOffset = -max(pdfView.currentYOffset, 0F)
        loadVisible()
    }
}
