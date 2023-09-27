package com.ismartcoding.lib.extensions

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.util.Patterns
import android.util.TypedValue
import android.view.View
import android.widget.TextView

fun TextView.setTextSizePx(px: Int) {
    setTextSize(TypedValue.COMPLEX_UNIT_PX, px.toFloat())
}

fun TextView.setTextWithLinkSupport(
    fullText: String,
    callback: (String) -> Unit,
) {
    val spannable = SpannableString(fullText)
    val matcher = Patterns.WEB_URL.matcher(spannable)
    while (matcher.find()) {
        val url = spannable.toString().substring(matcher.start(), matcher.end())
        val urlSpan =
            object : URLSpan(fullText) {
                override fun onClick(widget: View) {
                    callback(url)
                }
            }
        spannable.setSpan(urlSpan, matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    text = spannable
    movementMethod = LinkMovementMethod.getInstance()
}

fun TextView.setDrawableColor(color: Int) {
    for (drawable in this.compoundDrawablesRelative) {
        drawable?.mutate()
        drawable?.colorFilter =
            PorterDuffColorFilter(
                color, PorterDuff.Mode.SRC_IN,
            )
    }
}
