package com.ismartcoding.plain.ui.base

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import java.util.regex.Pattern

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
        val emailPattern = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}")
        val phonePattern = Pattern.compile("\\d{10,13}")
        val urlPattern = Pattern.compile("(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]*[-A-Za-z0-9+&@#/%=~_|]")

        val patterns = listOf(emailPattern, phonePattern, urlPattern)
        val tags = listOf("EMAIL", "PHONE", "URL")

        val parse = fun(input: String): LinkifyParseResult? {
            var r: LinkifyParseResult? = null
            for (i in patterns.indices) {
                val m = patterns[i].matcher(input)
                if (m.find()) {
                    if (r == null) {
                        r = LinkifyParseResult(m.group(), m.start(), m.end(), tags[i])
                    } else if (r.start > m.start()) {
                        r = LinkifyParseResult(m.group(), m.start(), m.end(), tags[i])
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