package com.ismartcoding.plain.ui.page.search

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

/** Marks every case-insensitive occurrence of [query] in [text] bold on a highlight background. */
fun highlightQuery(text: String, query: String, highlight: Color): AnnotatedString {
    if (text.isEmpty() || query.isEmpty()) return AnnotatedString(text)
    val lower = text.lowercase()
    val ql = query.lowercase()
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            val hit = lower.indexOf(ql, i)
            if (hit < 0) {
                append(text.substring(i))
                break
            }
            append(text.substring(i, hit))
            val end = hit + ql.length
            addStyle(
                SpanStyle(fontWeight = FontWeight.Bold, background = highlight),
                this.length,
                this.length + (end - hit),
            )
            append(text.substring(hit, end))
            i = end
        }
    }
}
