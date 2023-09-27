package com.ismartcoding.plain.ui.base

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.ismartcoding.plain.ui.helpers.WebHelper
import java.util.regex.Matcher
import java.util.regex.Pattern

fun String.linkify(linkStyle: SpanStyle) =
    buildAnnotatedString {
        val emailPattern = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}")
        val phonePattern = Pattern.compile("\\d{10,13}") // Adjust the pattern to match your phone number format
        val urlPattern = Pattern.compile("(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]*[-A-Za-z0-9+&@#/%=~_|]")

        val patterns = listOf(emailPattern, phonePattern, urlPattern)
        val tags = listOf("EMAIL", "PHONE", "URL")

        val parse =

            fun(input: String): Pair<Matcher, String>? {
                var matcher: Matcher? = null
                var tag = ""
                for (i in patterns.indices) {
                    val pattern = patterns[i]
                    val m = pattern.matcher(input)
                    if (m.find()) {
                        if (matcher == null) {
                            matcher = m
                            tag = tags[i]
                        } else if (matcher.start() > m.start()) {
                            matcher = m
                            tag = tags[i]
                        }
                    }
                }

                if (matcher != null) {
                    return Pair(matcher, tag)
                }

                return null
            }

        var raw = this@linkify
        var m = parse(raw)
        while (m != null) {
            val matcher = m.first
            val start = matcher.start()
            val end = matcher.end()
            val matchedText = raw.substring(start, end)

            if (start > 0) {
                // Append non-link text before the match
                append(raw.subSequence(0, start))
            }

            withStyle(linkStyle) {
                // Append the linked text
                addStringAnnotation(
                    tag = m!!.second,
                    annotation = matchedText,
                    start = length,
                    end = length + matchedText.length,
                )
                append(matchedText)
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
) {
    val annotations = getStringAnnotations(position, position)
    annotations.forEach {
        when (it.tag) {
            "URL" -> {
                WebHelper.open(context, it.item)
                return
            }

            "EMAIL" -> {
                val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${it.item}"))
                context.startActivity(emailIntent)
                return
            }

            "PHONE" -> {
                val phoneIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${it.item}"))
                context.startActivity(phoneIntent)
                return
            }
        }
    }
}
