package com.ismartcoding.lib.pdfviewer

import android.graphics.*
import android.util.SparseBooleanArray
import com.ismartcoding.lib.pdfviewer.exception.PageRenderingException
import com.ismartcoding.lib.pdfviewer.util.FitPolicy
import com.ismartcoding.lib.pdfviewer.util.PageSizeCalculator
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfDocument.Bookmark
import com.shockwave.pdfium.PdfDocument.Meta
import com.shockwave.pdfium.PdfiumCore
import com.shockwave.pdfium.util.Size
import com.shockwave.pdfium.util.SizeF
import kotlin.math.max

class PdfFile(
    private val pdfiumCore: PdfiumCore?,
    private var pdfDocument: PdfDocument?,
    private val pageFitPolicy: FitPolicy,
    viewSize: Size,
    /**
     * The pages the user want to display in order
     * (ex: 0, 2, 2, 8, 8, 1, 1, 1)
     */
    private var originalUserPages: IntArray?,
    /** True if scrolling is vertical, else it's horizontal  */
    private val isVertical: Boolean,
    /** Fixed spacing between pages in pixels  */
    private val spacingPx: Int,
    /** Calculate spacing automatically so each page fits on it's own in the center of the view  */
    private val autoSpacing: Boolean,
    /**
     * True if every page should fit separately according to the FitPolicy,
     * else the largest page fits and other pages scale relatively
     */
    private val fitEachPage: Boolean,
) {
    var pagesCount = 0
        private set

    /** Original page sizes  */
    private val originalPageSizes: MutableList<Size> = ArrayList()

    /** Scaled page sizes  */
    private val pageSizes: MutableList<SizeF> = ArrayList()

    /** Opened pages with indicator whether opening was successful  */
    private val openedPages = SparseBooleanArray()

    /** Page with maximum width  */
    private var originalMaxWidthPageSize = Size(0, 0)

    /** Page with maximum height  */
    private var originalMaxHeightPageSize = Size(0, 0)

    /** Scaled page with maximum height  */
    private var maxHeightPageSize = SizeF(0F, 0F)

    /** Scaled page with maximum width  */
    private var maxWidthPageSize = SizeF(0F, 0F)

    /** Calculated offsets for pages  */
    private val pageOffsets: MutableList<Float> = ArrayList()

    /** Calculated auto spacing for pages  */
    private val pageSpacing: MutableList<Float> = ArrayList()

    /** Calculated document length (width or height, depending on swipe mode)  */
    private var documentLength = 0f

    init {
        setup(viewSize)
    }

    private fun setup(viewSize: Size) {
        pagesCount =
            if (originalUserPages != null) {
                originalUserPages!!.size
            } else {
                pdfiumCore!!.getPageCount(pdfDocument)
            }
        for (i in 0 until pagesCount) {
            val pageSize = pdfiumCore!!.getPageSize(pdfDocument, documentPage(i))
            if (pageSize.width > originalMaxWidthPageSize.width) {
                originalMaxWidthPageSize = pageSize
            }
            if (pageSize.height > originalMaxHeightPageSize.height) {
                originalMaxHeightPageSize = pageSize
            }
            originalPageSizes.add(pageSize)
        }
        recalculatePageSizes(viewSize)
    }

    /**
     * Call after view size change to recalculate page sizes, offsets and document length
     *
     * @param viewSize new size of changed view
     */
    fun recalculatePageSizes(viewSize: Size) {
        pageSizes.clear()
        val calculator =
            PageSizeCalculator(
                pageFitPolicy,
                originalMaxWidthPageSize,
                originalMaxHeightPageSize,
                viewSize,
                fitEachPage,
            )
        maxWidthPageSize = calculator.optimalMaxWidthPageSize
        maxHeightPageSize = calculator.optimalMaxHeightPageSize
        for (size in originalPageSizes) {
            pageSizes.add(calculator.calculate(size))
        }
        if (autoSpacing) {
            prepareAutoSpacing(viewSize)
        }
        prepareDocLen()
        preparePagesOffset()
    }

    fun getPageSize(pageIndex: Int): SizeF {
        val docPage = documentPage(pageIndex)
        return if (docPage < 0) {
            SizeF(0F, 0F)
        } else {
            pageSizes[pageIndex]
        }
    }

    fun getScaledPageSize(
        pageIndex: Int,
        zoom: Float,
    ): SizeF {
        val size = getPageSize(pageIndex)
        return SizeF(size.width * zoom, size.height * zoom)
    }

    /**
     * get page size with biggest dimension (width in vertical mode and height in horizontal mode)
     *
     * @return size of page
     */
    val maxPageSize: SizeF
        get() = if (isVertical) maxWidthPageSize else maxHeightPageSize
    val maxPageWidth: Float
        get() = maxPageSize.width
    val maxPageHeight: Float
        get() = maxPageSize.height

    private fun prepareAutoSpacing(viewSize: Size) {
        pageSpacing.clear()
        for (i in 0 until pagesCount) {
            val pageSize = pageSizes[i]
            var spacing = max(0f, if (isVertical) viewSize.height - pageSize.height else viewSize.width - pageSize.width)
            if (i < pagesCount - 1) {
                spacing += spacingPx.toFloat()
            }
            pageSpacing.add(spacing)
        }
    }

    private fun prepareDocLen() {
        var length = 0f
        for (i in 0 until pagesCount) {
            val pageSize = pageSizes[i]
            length += if (isVertical) pageSize.height else pageSize.width
            if (autoSpacing) {
                length += pageSpacing[i]
            } else if (i < pagesCount - 1) {
                length += spacingPx.toFloat()
            }
        }
        documentLength = length
    }

    private fun preparePagesOffset() {
        pageOffsets.clear()
        var offset = 0f
        for (i in 0 until pagesCount) {
            val pageSize = pageSizes[i]
            val size = if (isVertical) pageSize.height else pageSize.width
            if (autoSpacing) {
                offset += pageSpacing[i] / 2f
                if (i == 0) {
                    offset -= spacingPx / 2f
                } else if (i == pagesCount - 1) {
                    offset += spacingPx / 2f
                }
                pageOffsets.add(offset)
                offset += size + pageSpacing[i] / 2f
            } else {
                pageOffsets.add(offset)
                offset += size + spacingPx
            }
        }
    }

    fun getDocLen(zoom: Float): Float {
        return documentLength * zoom
    }

    /**
     * Get the page's height if swiping vertical, or width if swiping horizontal.
     */
    fun getPageLength(
        pageIndex: Int,
        zoom: Float,
    ): Float {
        val size = getPageSize(pageIndex)
        return (if (isVertical) size.height else size.width) * zoom
    }

    fun getPageSpacing(
        pageIndex: Int,
        zoom: Float,
    ): Float {
        val spacing = if (autoSpacing) pageSpacing[pageIndex] else spacingPx.toFloat()
        return spacing * zoom
    }

    /** Get primary page offset, that is Y for vertical scroll and X for horizontal scroll  */
    fun getPageOffset(
        pageIndex: Int,
        zoom: Float,
    ): Float {
        val docPage = documentPage(pageIndex)
        return if (docPage < 0) {
            0F
        } else {
            pageOffsets[pageIndex] * zoom
        }
    }

    /** Get secondary page offset, that is X for vertical scroll and Y for horizontal scroll  */
    fun getSecondaryPageOffset(
        pageIndex: Int,
        zoom: Float,
    ): Float {
        val pageSize = getPageSize(pageIndex)
        return if (isVertical) {
            val maxWidth = maxPageWidth
            zoom * (maxWidth - pageSize.width) / 2 // x
        } else {
            val maxHeight = maxPageHeight
            zoom * (maxHeight - pageSize.height) / 2 // y
        }
    }

    fun getPageAtOffset(
        offset: Float,
        zoom: Float,
    ): Int {
        var currentPage = 0
        for (i in 0 until pagesCount) {
            val off = pageOffsets[i] * zoom - getPageSpacing(i, zoom) / 2f
            if (off >= offset) {
                break
            }
            currentPage++
        }
        return if (--currentPage >= 0) currentPage else 0
    }

    @Throws(PageRenderingException::class)
    fun openPage(pageIndex: Int): Boolean {
        val docPage = documentPage(pageIndex)
        if (docPage < 0) {
            return false
        }
        synchronized(lock) {
            return if (openedPages.indexOfKey(docPage) < 0) {
                try {
                    pdfiumCore!!.openPage(pdfDocument, docPage)
                    openedPages.put(docPage, true)
                    true
                } catch (e: Exception) {
                    openedPages.put(docPage, false)
                    throw PageRenderingException(pageIndex, e)
                }
            } else {
                false
            }
        }
    }

    fun pageHasError(pageIndex: Int): Boolean {
        val docPage = documentPage(pageIndex)
        return !openedPages[docPage, false]
    }

    fun renderPageBitmap(
        bitmap: Bitmap,
        pageIndex: Int,
        bounds: Rect,
        annotationRendering: Boolean,
    ) {
        val docPage = documentPage(pageIndex)
        pdfiumCore!!.renderPageBitmap(
            pdfDocument,
            bitmap,
            docPage,
            bounds.left,
            bounds.top,
            bounds.width(),
            bounds.height(),
            annotationRendering,
        )
    }

    val metaData: Meta?
        get() =
            if (pdfDocument == null) {
                null
            } else {
                pdfiumCore!!.getDocumentMeta(pdfDocument)
            }
    val bookmarks: List<Bookmark>
        get() =
            if (pdfDocument == null) {
                ArrayList()
            } else {
                pdfiumCore!!.getTableOfContents(pdfDocument)
            }

    fun getPageLinks(pageIndex: Int): List<PdfDocument.Link> {
        val docPage = documentPage(pageIndex)
        return pdfiumCore!!.getPageLinks(pdfDocument, docPage)
    }

    fun mapRectToDevice(
        pageIndex: Int,
        startX: Int,
        startY: Int,
        sizeX: Int,
        sizeY: Int,
        rect: RectF?,
    ): RectF {
        val docPage = documentPage(pageIndex)
        return pdfiumCore!!.mapRectToDevice(pdfDocument, docPage, startX, startY, sizeX, sizeY, 0, rect)
    }

    fun dispose() {
        if (pdfiumCore != null && pdfDocument != null) {
            pdfiumCore.closeDocument(pdfDocument)
        }
        pdfDocument = null
        originalUserPages = null
    }

    /**
     * Given the UserPage number, this method restrict it
     * to be sure it's an existing page. It takes care of
     * using the user defined pages if any.
     *
     * @param userPage A page number.
     * @return A restricted valid page number (example : -2 => 0)
     */
    fun determineValidPageNumberFrom(userPage: Int): Int {
        if (userPage <= 0) {
            return 0
        }
        if (originalUserPages != null) {
            if (userPage >= originalUserPages!!.size) {
                return originalUserPages!!.size - 1
            }
        } else {
            if (userPage >= pagesCount) {
                return pagesCount - 1
            }
        }
        return userPage
    }

    fun documentPage(userPage: Int): Int {
        var documentPage = userPage
        if (originalUserPages != null) {
            if (userPage < 0 || userPage >= originalUserPages!!.size) {
                return -1
            } else {
                documentPage = originalUserPages!![userPage]
            }
        }

        if (documentPage < 0 || userPage >= pagesCount) {
            return -1
        }

        return documentPage
    }

    companion object {
        private val lock = Any()
    }
}
