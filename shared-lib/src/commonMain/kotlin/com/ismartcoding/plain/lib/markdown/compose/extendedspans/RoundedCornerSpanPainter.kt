// Copyright 2023, Saket Narayan
// SPDX-License-Identifier: Apache-2.0
// https://github.com/saket/extended-spans
package com.ismartcoding.plain.lib.markdown.compose.extendedspans

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import com.ismartcoding.plain.lib.markdown.compose.extendedspans.internal.deserializeToColor
import com.ismartcoding.plain.lib.markdown.compose.extendedspans.internal.serialize
import com.ismartcoding.plain.lib.markdown.compose.extendedspans.internal.update
import com.ismartcoding.plain.lib.markdown.utils.toPxOrZero

/**
 * Draws round rectangles behind text annotated using `SpanStyle(background = …)`.
 *
 * [topMargin] and [bottomMargin] are placeholder values that will be automatically calculated from font metrics
 * in the future once Compose UI starts exposing them ([Issue tracker](https://issuetracker.google.com/u/1/issues/237428541)).
 * In the meantime, you can calculate these depending upon your text's font size and line height.
 */
class RoundedCornerSpanPainter(
    private val cornerRadius: TextUnit = 8.sp,
    private val stroke: Stroke? = null,
    private val padding: TextPaddingValues = TextPaddingValues(horizontal = 2.sp, vertical = 2.sp),
    private val topMargin: TextUnit = 1.sp,
    private val bottomMargin: TextUnit = 1.sp,
) : ExtendedSpanPainter() {
    private val path = Path()

    override fun decorate(
        span: SpanStyle,
        start: Int,
        end: Int,
        text: AnnotatedString,
        builder: AnnotatedString.Builder,
    ): SpanStyle {
        return if (span.background.isUnspecified) {
            span
        } else {
            builder.addStringAnnotation(TAG, annotation = span.background.serialize(), start = start, end = end)
            span.copy(background = Color.Unspecified)
        }
    }

    override fun decorate(
        linkAnnotation: LinkAnnotation,
        start: Int,
        end: Int,
        text: AnnotatedString,
        builder: AnnotatedString.Builder,
    ): LinkAnnotation {
        val defaultStyle = linkAnnotation.styles?.style
        // return fast if background is not set
        if (defaultStyle == null || defaultStyle.background.isUnspecified) return linkAnnotation
        builder.addStringAnnotation(TAG, annotation = defaultStyle.background.serialize(), start = start, end = end)
        val updatedTextLinkStyles = linkAnnotation.styles?.update { copy(background = Color.Unspecified) }
        return when (linkAnnotation) {
            is LinkAnnotation.Url -> {
                LinkAnnotation.Url(linkAnnotation.url, updatedTextLinkStyles, linkAnnotation.linkInteractionListener)
            }
            is LinkAnnotation.Clickable -> {
                LinkAnnotation.Clickable(linkAnnotation.tag, updatedTextLinkStyles, linkAnnotation.linkInteractionListener)
            }
            else -> throw IllegalStateException("Unsupported LinkAnnotation type: $linkAnnotation")
        }
    }

    override fun drawInstructionsFor(layoutResult: TextLayoutResult, color: Color?): SpanDrawInstructions {
        val text = layoutResult.layoutInput.text
        val annotations = text.getStringAnnotations(TAG, start = 0, end = text.length)

        return SpanDrawInstructions {
            val cornerRadius = CornerRadius(toPxOrZero(cornerRadius))

            annotations.fastForEach { annotation ->
                val backgroundColor = annotation.item.deserializeToColor()!!
                val boxes = layoutResult.getBoundingBoxes(
                    startOffset = annotation.start,
                    endOffset = annotation.end,
                    flattenForFullParagraphs = true
                )
                boxes.fastForEachIndexed { index, box ->
                    path.rewind()
                    path.addRoundRect(
                        RoundRect(
                            rect = box.copy(
                                left = box.left - toPxOrZero(padding.horizontal),
                                right = box.right + toPxOrZero(padding.horizontal),
                                top = box.top - toPxOrZero(padding.vertical) + toPxOrZero(topMargin),
                                bottom = box.bottom + toPxOrZero(padding.vertical) - toPxOrZero(bottomMargin),
                            ),
                            topLeft = if (index == 0) cornerRadius else CornerRadius.Zero,
                            bottomLeft = if (index == 0) cornerRadius else CornerRadius.Zero,
                            topRight = if (index == boxes.lastIndex) cornerRadius else CornerRadius.Zero,
                            bottomRight = if (index == boxes.lastIndex) cornerRadius else CornerRadius.Zero
                        )
                    )
                    drawPath(
                        path = path,
                        color = backgroundColor,
                        style = Fill
                    )
                    if (stroke != null) {
                        drawPath(
                            path = path,
                            color = stroke.color(backgroundColor),
                            style = Stroke(
                                width = toPxOrZero(stroke.width),
                            )
                        )
                    }
                }
            }
        }
    }

    data class Stroke(
        val color: (background: Color) -> Color,
        val width: TextUnit = 1.sp,
    ) {
        constructor(color: Color, width: TextUnit = 1.sp) : this(
            color = { color },
            width = width
        )
    }

    data class TextPaddingValues(
        val horizontal: TextUnit = 0.sp,
        val vertical: TextUnit = 0.sp,
    )

    companion object {
        private const val TAG = "rounded_corner_span"
    }
}