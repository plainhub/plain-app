package com.ismartcoding.plain.ui.base

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.ismartcoding.plain.R
import com.ismartcoding.plain.packageManager
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.helpers.WebHelper
import com.ismartcoding.plain.ui.models.VClickText

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
