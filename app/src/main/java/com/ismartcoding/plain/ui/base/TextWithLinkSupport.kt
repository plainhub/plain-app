package com.ismartcoding.plain.ui.base

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.ismartcoding.plain.ui.helpers.WebHelper
import java.util.regex.Pattern

fun String.linkify(
    linkStyle: SpanStyle,
) = buildAnnotatedString {
    val emailPattern = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}")
    val phonePattern = Pattern.compile("\\d{10,13}") // Adjust the pattern to match your phone number format
    val urlPattern = Pattern.compile("(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]*[-A-Za-z0-9+&@#/%=~_|]")

    val patterns = listOf(emailPattern, phonePattern, urlPattern)
    val tags = listOf("EMAIL", "PHONE", "URL")

    var r = this@linkify
    for (i in patterns.indices) {
        val pattern = patterns[i]
        val tag = tags[i]

        val matcher = pattern.matcher(r)

        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            val matchedText = r.substring(start, end)

            if (start > 0) {
                // Append non-link text before the match
                append(r.subSequence(0, start))
            }

            withStyle(linkStyle) {
                // Append the linked text
                addStringAnnotation(
                    tag = tag,
                    annotation = matchedText,
                    start = length,
                    end = length + matchedText.length
                )
                append(matchedText)
            }

            // Remove the processed part of the text
            r = r.substring(end)
        }
    }

    // Append any remaining non-link text
    append(r)
}

fun AnnotatedString.urlAt(context: Context, position: Int) {
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
