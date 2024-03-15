package com.ismartcoding.plain.ui.base

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.ismartcoding.plain.R
import com.ismartcoding.plain.packageManager
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.helpers.WebHelper
import com.ismartcoding.plain.ui.models.VClickText
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
        val phonePattern = Pattern.compile("\\d{10,13}") // Adjust the pattern to match your phone number format
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
                // Append non-link text before the match
                append(raw.subSequence(0, start))
            }

            withStyle(linkStyle) {
                // Append the linked text
                addStringAnnotation(
                    tag = m!!.tag,
                    annotation = m!!.text,
                    start = length,
                    end = length + m!!.text.length,
                )
                append(m!!.text)
            }

            // Remove the processed part of the text
            raw = raw.substring(end)
            m = parse(raw)
        }

        // Append any remaining non-link text
        append(raw)
    }

fun AnnotatedString.urlAt(
    context: Context,
    position: Int,
): Boolean {
    val annotations = getStringAnnotations(position, position)
    annotations.forEach {
        when (it.tag) {
            "URL" -> {
                WebHelper.open(context, it.item)
                return true
            }

            "EMAIL" -> {
                val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${it.item}"))
                if (emailIntent.resolveActivity(packageManager) != null) {
                    context.startActivity(emailIntent)
                } else {
                    DialogHelper.showMessage(R.string.not_supported_error)
                }
                return true

            }

            "PHONE" -> {
                val phoneIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${it.item}"))
                if (phoneIntent.resolveActivity(packageManager) != null) {
                    context.startActivity(phoneIntent)
                } else {
                    DialogHelper.showMessage(R.string.not_supported_error)
                }
                return true
            }
        }
    }

    return false
}

@Composable
fun String.linkify(
    clickTexts: List<VClickText>, linkStyle: SpanStyle = SpanStyle(
        color = MaterialTheme.colorScheme.primary,
    )
) = buildAnnotatedString {
    val parse = fun(input: String): LinkifyParseResult? {
        var r: LinkifyParseResult? = null
        var tag = ""
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
            // Append non-link text before the match
            append(raw.subSequence(0, start))
        }

        withStyle(linkStyle) {
            // Append the linked text
            addStringAnnotation(
                tag = m!!.tag,
                annotation = m!!.text,
                start = length,
                end = length + m!!.text.length,
            )
            append(m!!.text)
        }

        // Remove the processed part of the text
        raw = raw.substring(end)
        m = parse(raw)
    }

    // Append any remaining non-link text
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