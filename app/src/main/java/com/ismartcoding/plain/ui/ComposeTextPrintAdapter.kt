package com.ismartcoding.plain.ui

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextMeasurer
import androidx.core.util.TypedValueCompat.spToPx
import java.io.FileOutputStream

class ComposeTextPrintAdapter(
    private val context: Context,
    private val textMeasurer: TextMeasurer,
    private val title: String,
    private val content: String,
) : PrintDocumentAdapter() {

    companion object {
        // A4 size in PDF points
        private const val PAGE_WIDTH = 595
        private const val PAGE_HEIGHT = 842

        private const val MARGIN = 48

        private const val CONTENT_WIDTH =
            PAGE_WIDTH - MARGIN * 2

        private const val CONTENT_HEIGHT =
            PAGE_HEIGHT - MARGIN * 2

        private const val HEADER_HEIGHT = 32
        private const val FOOTER_HEIGHT = 32

        private const val TEXT_TOP =
            MARGIN + HEADER_HEIGHT

        private const val TEXT_HEIGHT =
            CONTENT_HEIGHT - HEADER_HEIGHT - FOOTER_HEIGHT
    }

    private lateinit var layoutResult: TextLayoutResult

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: android.os.Bundle?
    ) {

        layoutResult = textMeasurer.measure(
            text = AnnotatedString(content),
            style = TextStyle(
                fontSize = 11.sp,
                lineHeight = 18.sp,
                color = Color(0xFF222222),
                fontFamily = FontFamily.SansSerif
            ),
            constraints = Constraints(
                maxWidth = CONTENT_WIDTH
            )
        )

        val pageCount = calculatePages(layoutResult)

        callback?.onLayoutFinished(
            PrintDocumentInfo.Builder("$title.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(pageCount)
                .build(),
            true
        )
    }

    override fun onWrite(
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
    ) {

        val pdf = PdfDocument()

        val totalPages = calculatePages(layoutResult)

        var currentLine = 0

        for (pageIndex in 0 until totalPages) {

            val pageInfo = PdfDocument.PageInfo.Builder(
                PAGE_WIDTH,
                PAGE_HEIGHT,
                pageIndex + 1
            ).create()

            val page = pdf.startPage(pageInfo)

            val canvas = page.canvas

            // ===== Header =====

            val headerPaint = android.graphics.Paint().apply {
                textSize = 11f
                color = Color.Gray.toArgb()
                isAntiAlias = true
            }

            canvas.drawText(
                title,
                MARGIN.toFloat(),
                (MARGIN - 12).toFloat(),
                headerPaint
            )

            val textPaint = android.text.TextPaint().apply {
                textSize = 11f.spToPx()
                color = Color(0xFF222222).toArgb()
                isAntiAlias = true
            }

            val startLine = currentLine

            while (
                currentLine < layoutResult.lineCount &&
                layoutResult.getLineBottom(currentLine) -
                layoutResult.getLineTop(startLine) < TEXT_HEIGHT
            ) {
                currentLine++
            }

            val endLine = currentLine

            drawLines(
                canvas = canvas,
                layout = layoutResult,
                text = content,
                paint = textPaint,
                startLine = startLine,
                endLine = endLine
            )

            // ===== Footer =====

            canvas.drawText(
                "Page ${pageIndex + 1}",
                (PAGE_WIDTH - MARGIN - 60).toFloat(),
                (PAGE_HEIGHT - MARGIN / 2).toFloat(),
                headerPaint
            )

            pdf.finishPage(page)
        }

        try {
            FileOutputStream(destination.fileDescriptor).use {
                pdf.writeTo(it)
            }

            callback?.onWriteFinished(
                arrayOf(PageRange.ALL_PAGES)
            )
        } catch (e: Exception) {
            callback?.onWriteFailed(e.message)
        } finally {
            pdf.close()
        }
    }

    private fun calculatePages(
        layout: TextLayoutResult
    ): Int {

        var pages = 1
        var startLine = 0

        while (startLine < layout.lineCount) {

            var currentLine = startLine

            while (
                currentLine < layout.lineCount &&
                layout.getLineBottom(currentLine) -
                layout.getLineTop(startLine) < TEXT_HEIGHT
            ) {
                currentLine++
            }

            if (currentLine < layout.lineCount) {
                pages++
            }

            startLine = currentLine
        }

        return pages
    }

    private fun drawLines(
        canvas: android.graphics.Canvas,
        layout: TextLayoutResult,
        text: String,
        paint: android.text.TextPaint,
        startLine: Int,
        endLine: Int
    ) {

        val startOffset =
            layout.getLineStart(startLine)

        val endOffset =
            layout.getLineEnd(endLine - 1)

        val pageText =
            text.substring(startOffset, endOffset)

        val staticLayout =
            android.text.StaticLayout.Builder
                .obtain(
                    pageText,
                    0,
                    pageText.length,
                    paint,
                    CONTENT_WIDTH
                )
                .setLineSpacing(8f, 1f)
                .build()

        canvas.save()

        canvas.translate(
            MARGIN.toFloat(),
            TEXT_TOP.toFloat()
        )

        staticLayout.draw(canvas)

        canvas.restore()
    }

    private fun Float.spToPx(): Float {
        return this * context.resources.displayMetrics.scaledDensity
    }
}