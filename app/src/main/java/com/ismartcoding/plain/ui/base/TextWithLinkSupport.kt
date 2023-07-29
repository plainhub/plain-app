package com.ismartcoding.plain.ui.base

import android.content.Context
import android.text.SpannableString
import android.text.style.URLSpan
import android.text.util.Linkify
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.core.text.util.LinkifyCompat
import com.ismartcoding.plain.ui.helpers.WebHelper

fun String.linkify(
    linkStyle: SpanStyle,
) = buildAnnotatedString {
    append(this@linkify)

    val spannable = SpannableString(this@linkify)
    Linkify.addLinks(spannable, Linkify.WEB_URLS)

    val spans = spannable.getSpans(0, spannable.length, URLSpan::class.java)
    for (span in spans) {
        val start = spannable.getSpanStart(span)
        val end = spannable.getSpanEnd(span)

        addStyle(
            start = start,
            end = end,
            style = linkStyle,
        )
        addStringAnnotation(
            tag = "URL",
            annotation = span.url,
            start = start,
            end = end
        )
    }
}

fun AnnotatedString.urlAt(context: Context, position: Int) =
    getStringAnnotations("URL", position, position).firstOrNull()?.item?.let {
        WebHelper.open(context, it)
    }