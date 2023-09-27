package com.ismartcoding.plain.ui.extensions

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.view.MenuItem
import com.ismartcoding.lib.extensions.*
import com.ismartcoding.plain.R

fun MenuItem.setTitle(
    context: Context,
    title: String,
) {
    SpannableString(title).apply {
        setSpan(RelativeSizeSpan(0.85f), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        setColor(context, R.color.purple)
        setTitle(this)
    }
}

fun MenuItem.unhighlightTitle() {
    val t = intent?.getStringExtra("title")
    if (t != null) {
        title = t
    }
}

fun MenuItem.highlightTitle(context: Context) {
    if (intent == null) {
        intent =
            Intent().apply {
                putExtra("title", title)
            }
    }
    title =
        SpannableString(title).apply {
            setTypeface(Typeface.BOLD)
        }
}
