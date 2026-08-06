// Copyright 2023, Saket Narayan
// SPDX-License-Identifier: Apache-2.0
// https://github.com/saket/extended-spans
package com.ismartcoding.plain.lib.markdown.compose.extendedspans

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.ResolvedTextDirection.Ltr
import com.ismartcoding.plain.lib.markdown.compose.extendedspans.internal.fastMapRange

abstract class ExtendedSpanPainter {
    /**
     * Can be used for removing any existing spans from [text] so that they can be drawn manually.
     */
    abstract fun decorate(
        span: SpanStyle,
        start: Int,
        end: Int,
        text: AnnotatedString,
        builder: AnnotatedString.Builder,
    ): SpanStyle

    /**
     * Can be used for removing any existing text link styles from [text] so that they can be drawn manually.
     */
    abstract fun decorate(
        linkAnnotation: LinkAnnotation,
        start: Int,
        end: Int,
        text: AnnotatedString,
        builder: AnnotatedString.Builder,
    ): LinkAnnotation

    abstract fun drawInstructionsFor(
        layoutResult: TextLayoutResult,
        color: Color? = null,
    ): SpanDrawInstructions

    /**
     * Reads bounds for multiple lines. This can be removed once an
     * [official API](https://issuetracker.google.com/u/1/issues/237289433) is released.
     *
     * When [flattenForFullParagraphs] is available, the bounds for one or multiple
     * entire paragraphs is returned instead of separate lines if [startOffset]
     * and [endOffset] represent the extreme ends of those paragraph.
     */
    protected fun TextLayoutResult.getBoundingBoxes(
        startOffset: Int,
        endOffset: Int,
        flattenForFullParagraphs: Boolean = false,
    ): List<Rect> {
        if (startOffset == endOffset) {
            return emptyList()
        }

        val startLineNum = getLineForOffset(startOffset)
        val endLineNum = getLineForOffset(endOffset)

        if (flattenForFullParagraphs) {
            val isFullParagraph = (startLineNum != endLineNum)
                    && getLineStart(startLineNum) == startOffset
                    && multiParagraph.getLineEnd(endLineNum, visibleEnd = true) == endOffset

            if (isFullParagraph) {
                return listOf(
                    Rect(
                        top = getLineTop(startLineNum),
                        bottom = getLineBottom(endLineNum),
                        left = 0f,
                        right = size.width.toFloat()
                    )
                )
            }
        }

        // Compose UI does not offer any API for reading paragraph direction for an entire line.
        // So this code assumes that all paragraphs in the text will have the same direction.
        // It also assumes that this paragraph does not contain bi-directional text.
        val isLtr = multiParagraph.getParagraphDirection(offset = layoutInput.text.lastIndex) == Ltr

        return fastMapRange(startLineNum, endLineNum) { lineNum ->
            Rect(
                top = getLineTop(lineNum),
                bottom = getLineBottom(lineNum),
                left = if (lineNum == startLineNum) {
                    getHorizontalPosition(startOffset, usePrimaryDirection = isLtr)
                } else {
                    getLineLeft(lineNum)
                },
                right = if (lineNum == endLineNum) {
                    getHorizontalPosition(endOffset, usePrimaryDirection = isLtr)
                } else {
                    getLineRight(lineNum)
                }
            )
        }
    }
}

fun interface SpanDrawInstructions {
    fun DrawScope.draw()
}