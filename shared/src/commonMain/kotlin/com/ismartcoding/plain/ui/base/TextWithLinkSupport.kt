package com.ismartcoding.plain.ui.base

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.ismartcoding.plain.ui.models.VClickText

data class LinkifyParseResult(
    val text: String,
    val start: Int,
    val end: Int,
    val tag: String = "",
)

@Composable
fun String.linkify(
    linkStyle: SpanStyle = SpanStyle(
        color = MaterialTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline,
    )
) =
    buildAnnotatedString {
        val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}")
        val phoneRegex = Regex("\\d{10,13}")
        val urlRegex = Regex("(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]*[-A-Za-z0-9+&@#/%=~_|]")

        val patterns = listOf(emailRegex, phoneRegex, urlRegex)
        val tags = listOf("EMAIL", "PHONE", "URL")

        val parse = fun(input: String): LinkifyParseResult? {
            var r: LinkifyParseResult? = null
            for (i in patterns.indices) {
                val m = patterns[i].find(input)
                if (m != null) {
                    if (r == null) {
                        r = LinkifyParseResult(m.value, m.range.first, m.range.last + 1, tags[i])
                    } else if (r.start > m.range.first) {
                        r = LinkifyParseResult(m.value, m.range.first, m.range.last + 1, tags[i])
                    }
                }
            }

            return r
        }

        var raw = this@linkify
        var m = parse(raw)
        while (m != null) {
            val start = m.start
            val end = m.end

            if (start > 0) {
                append(raw.subSequence(0, start))
            }

            withStyle(linkStyle) {
                addStringAnnotation(
                    tag = m.tag,
                    annotation = m.text,
                    start = length,
                    end = length + m.text.length,
                )
                append(m.text)
            }

            raw = raw.substring(end)
            m = parse(raw)
        }

        append(raw)
    }

@Composable
fun String.linkify(
    clickTexts: List<VClickText>,
    linkStyle: SpanStyle = SpanStyle(
        color = MaterialTheme.colorScheme.primary,
    ),
) = buildAnnotatedString {
    val parse = fun(input: String): LinkifyParseResult? {
        var r: LinkifyParseResult? = null
        clickTexts.forEach { v ->
            val startIndex = input.indexOf(v.text)
            if (startIndex != -1) {
                val endIndex = startIndex + v.text.length
                if (r == null) {
                    r = LinkifyParseResult(v.text, startIndex, endIndex)
                } else if (r!!.start > startIndex) {
                    r = LinkifyParseResult(v.text, startIndex, endIndex)
                }
            }
        }

        return r
    }

    var raw = this@linkify
    var m = parse(raw)
    while (m != null) {
        val start = m.start
        val end = m.end

        if (start > 0) {
            append(raw.subSequence(0, start))
        }

        withStyle(linkStyle) {
            addStringAnnotation(
                tag = m!!.tag,
                annotation = m!!.text,
                start = length,
                end = length + m!!.text.length,
            )
            append(m!!.text)
        }

        raw = raw.substring(end)
        m = parse(raw)
    }

    append(raw)
}

fun AnnotatedString.clickAt(
    position: Int,
    clickTexts: List<VClickText>,
): Boolean {
    val annotations = getStringAnnotations(position, position)
    annotations.forEach { a ->
        val v = clickTexts.find { it.text == a.item }
        if (v != null) {
            v.click()
            return true
        }
    }
    return false
}
