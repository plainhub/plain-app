package com.ismartcoding.plain.ui.base.mdeditor

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

object MdEditorHighlightHelper {
    @Composable
    fun highlight(text: String, fileExtension: String, firstColoredIndex: Int): AnnotatedString {
        return buildAnnotatedString {
            val highlightDriver = HighlightDriver(fileExtension)
            val highlights = highlightDriver.highlightText(text, firstColoredIndex)
            append(text)
            for ((color, start, end) in highlights) {
                addStyle(style = SpanStyle(color = color), start = start, end = end)
            }
        }
    }
}