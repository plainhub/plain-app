// Copyright 2023, Saket Narayan
// SPDX-License-Identifier: Apache-2.0
// https://github.com/saket/extended-spans
@file:OptIn(ExperimentalTextApi::class)

package com.ismartcoding.plain.lib.markdown.compose.extendedspans

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.util.fastFold
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap

@Stable
class ExtendedSpans(
    vararg painters: ExtendedSpanPainter,
) {
    private val painters = painters.toList()
    internal var drawInstructions by mutableStateOf<List<SpanDrawInstructions>>(emptyList())
        private set

    /**
     * Prepares [text] to be rendered by [painters]. [RoundedCornerSpanPainter] and [SquigglyUnderlineSpanPainter]
     * use this for removing background and underline spans so that they can be drawn manually.
     */
    fun extend(text: AnnotatedString): AnnotatedString {
        return buildAnnotatedString {
            append(text.text)

            // For onTextLayout to be called if a new instance of ExtendedSpans is applied with the same text.
            val uniqueKey = this@ExtendedSpans.hashCode().toString()
            addStringAnnotation(
                EXTENDED_SPANS_MARKER_TAG,
                annotation = uniqueKey,
                start = 0,
                end = 0
            )

            text.spanStyles.fastForEach {
                val decorated = painters.fastFold(initial = it.item) { updated, painter ->
                    painter.decorate(updated, it.start, it.end, text = text, builder = this)
                }
                addStyle(decorated, it.start, it.end)
            }
            text.paragraphStyles.fastForEach {
                addStyle(it.item, it.start, it.end)
            }
            text.getStringAnnotations(start = 0, end = text.length).fastForEach {
                addStringAnnotation(
                    tag = it.tag,
                    annotation = it.item,
                    start = it.start,
                    end = it.end
                )
            }
            text.getTtsAnnotations(start = 0, end = text.length).fastForEach {
                addTtsAnnotation(it.item, it.start, it.end)
            }
            @Suppress("DEPRECATION")
            text.getUrlAnnotations(start = 0, end = text.length).fastForEach {
                addUrlAnnotation(it.item, it.start, it.end)
            }
            text.getLinkAnnotations(start = 0, end = text.length).fastForEach { range ->
                val decorated = painters.fastFold(initial = range.item) { updated, painter ->
                    painter.decorate(updated, range.start, range.end, text = text, builder = this)
                }

                when (decorated) {
                    is LinkAnnotation.Url -> addLink(decorated, range.start, range.end)
                    is LinkAnnotation.Clickable -> addLink(decorated, range.start, range.end)
                }
            }
        }
    }

    fun onTextLayout(layoutResult: TextLayoutResult, color: Color? = null) {
        layoutResult.checkIfExtendWasCalled()
        drawInstructions = painters.fastMap {
            it.drawInstructionsFor(layoutResult, color)
        }
    }

    private fun TextLayoutResult.checkIfExtendWasCalled() {
        val wasExtendCalled = layoutInput.text.getStringAnnotations(
            tag = EXTENDED_SPANS_MARKER_TAG,
            start = 0,
            end = 0
        ).isNotEmpty()
        check(wasExtendCalled) {
            "ExtendedSpans#extend(AnnotatedString) wasn't called for this Text()."
        }
    }

    companion object {
        private const val EXTENDED_SPANS_MARKER_TAG = "extended_spans_marker"
    }
}

fun Modifier.drawBehind(spans: ExtendedSpans): Modifier {
    return drawBehind {
        spans.drawInstructions.fastForEach { instructions ->
            with(instructions) {
                draw()
            }
        }
    }
}
