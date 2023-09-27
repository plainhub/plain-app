package com.ismartcoding.plain.ui.views.texteditor

import android.content.Context
import com.ismartcoding.lib.extensions.dp2px

object EditTextPadding {
    fun getPaddingWithoutLineNumbers(context: Context): Int {
        return context.dp2px(8)
    }

    fun getPaddingWithLineNumbers(
        context: Context,
        fontSize: Int,
    ): Int {
        return context.dp2px(fontSize * 2)
    }

    fun getPaddingTop(context: Context): Int {
        return context.dp2px(8)
    }
}
