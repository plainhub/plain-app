// Copyright 2023, Saket Narayan
// SPDX-License-Identifier: Apache-2.0
// https://github.com/saket/extended-spans
package com.ismartcoding.plain.lib.markdown.compose.extendedspans

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
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
import androidx.compose.ui.text.TextStyle
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
 * When [useSpanMetricsForVertical] is `true` (default), the painter ignores
 * [TextLayoutResult.getBoundingBoxes]' vertical extent (which reflects the paragraph line
 * height) and instead derives `top`/`bottom` from the annotated span's own
 * [fontSize] + approximated ascent/descent. This keeps inline code (or any span with a smaller
 * font size than the paragraph line) visually centered inside its background instead of
 * having giant top padding caused by the much larger paragraph line-height.
 *
 * [topMargin] and [bottomMargin] are additional offsets applied on top of the metrics-based
 * calculation. Use them for fine-tuning after the metrics-based box already looks right.
 */
class RoundedCornerSpanPainter(
    private val cornerRadius: TextUnit = 8.sp,
    private val stroke: Stroke? = null,
    private val padding: TextPaddingValues = TextPaddingValues(horizontal = 2.sp, vertical = 2.sp),
    private val topMargin: TextUnit = 0.sp,
    private val bottomMargin: TextUnit = 0.sp,
    private val useSpanMetricsForVertical: Boolean = true,
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
        val layoutStyle = layoutResult.layoutInput.style

        return SpanDrawInstructions {
            val cornerRadiusPx = CornerRadius(toPxOrZero(cornerRadius))
            val horizontalPadPx = toPxOrZero(padding.horizontal)
            val verticalPadPx = toPxOrZero(padding.vertical)
            val topMarginPx = toPxOrZero(topMargin)
            val bottomMarginPx = toPxOrZero(bottomMargin)

            annotations.fastForEach { annotation ->
                val backgroundColor = annotation.item.deserializeToColor() ?: return@fastForEach
                val boxes = layoutResult.getBoundingBoxes(
                    startOffset = annotation.start,
                    endOffset = annotation.end,
                    flattenForFullParagraphs = true
                )

                // Resolve the SpanStyle that applies at the annotation range, then merge with
                // layout-level defaults. This gives us the real fontSize the span is drawn with
                // (e.g. inline code's 14.sp inside a 16.sp paragraph), independent of the
                // paragraph's line-height.
                val spanStyle = text.resolveSpanStyle(
                    start = annotation.start,
                    end = annotation.end,
                    layoutStyle = layoutStyle,
                )
                val fontSizePx = spanStyle.fontSize.toPx()
                // Approximated ascent/descent as a fraction of fontSize. Values chosen to
                // match typical Sans-Serif / Mono Latin fonts on Android & Desktop, which
                // sit on a ~78% / 22% split of cap+ascender / descender.
                val ascentPx = fontSizePx * 0.78f
                val descentPx = fontSizePx * 0.22f

                boxes.fastForEachIndexed { index, box ->
                    val rect = if (useSpanMetricsForVertical) {
                        // Use the real baseline from TextLayoutResult instead of a hardcoded
                        // 78% estimate. getLineForOffset maps the annotation offset to its
                        // containing line index, then getLineBaseline gives the exact Y
                        // coordinate where the glyphs actually sit on that line.
                        val lineIndex = layoutResult.getLineForOffset(annotation.start)
                        val baselineY = layoutResult.getLineBaseline(lineIndex)
                        val glyphTop = baselineY - ascentPx
                        val glyphBottom = baselineY + descentPx

                        Rect(
                            left = box.left - horizontalPadPx,
                            right = box.right + horizontalPadPx,
                            top = glyphTop - verticalPadPx + topMarginPx,
                            bottom = glyphBottom + verticalPadPx - bottomMarginPx,
                        )
                    } else {
                        box.copy(
                            left = box.left - horizontalPadPx,
                            right = box.right + horizontalPadPx,
                            top = box.top - verticalPadPx + topMarginPx,
                            bottom = box.bottom + verticalPadPx - bottomMarginPx,
                        )
                    }

                    path.rewind()
                    path.addRoundRect(
                        RoundRect(
                            rect = rect,
                            topLeft = if (index == 0) cornerRadiusPx else CornerRadius.Zero,
                            bottomLeft = if (index == 0) cornerRadiusPx else CornerRadius.Zero,
                            topRight = if (index == boxes.lastIndex) cornerRadiusPx else CornerRadius.Zero,
                            bottomRight = if (index == boxes.lastIndex) cornerRadiusPx else CornerRadius.Zero
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
                            style = Stroke(width = toPxOrZero(stroke.width))
                        )
                    }
                }
            }
        }
    }

    data class Stroke(
        val width: TextUnit = 1.sp,
        val color: (backgroundColor: Color) -> Color = { it.copy(alpha = 0.15f) },
    )

    data class TextPaddingValues(
        val horizontal: TextUnit = 2.sp,
        val vertical: TextUnit = 2.sp,
    )

    companion object {
        private const val TAG = "saket.inline_code_rounded_corner"
    }
}

/**
 * Find the union of all [AnnotatedString.spanStyles] ranges that overlap [start]/[end] and merge
 * them with the layout-level TextStyle defaults.
 */
private fun AnnotatedString.resolveSpanStyle(
    start: Int,
    end: Int,
    layoutStyle: TextStyle,
): TextStyle {
    var mergedFontSize = layoutStyle.fontSize
    var mergedFontWeight = layoutStyle.fontWeight
    spanStyles.fastForEach { range ->
        if (range.end > start && range.start < end) {
            if (range.item.fontSize != TextUnit.Unspecified) mergedFontSize = range.item.fontSize
            if (range.item.fontWeight != null) mergedFontWeight = range.item.fontWeight
        }
    }
    return TextStyle(
        fontSize = mergedFontSize,
        fontWeight = mergedFontWeight,
        fontFamily = layoutStyle.fontFamily,
    )
}
