package com.ismartcoding.lib.extensions

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat

fun SpannableString.setColor(
    context: Context,
    @ColorRes color: Int,
) {
    setSpan(
        ForegroundColorSpan(ContextCompat.getColor(context, color)),
        0,
        length,
        SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE,
    )
}

fun SpannableString.setTypeface(style: Int) {
    setSpan(StyleSpan(style), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
}
