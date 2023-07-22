package com.ismartcoding.plain.data

import android.content.Context

interface ISelectOption {
    fun isSelected(context: Context): Boolean
    fun getText(): String
}

interface IFormItem {
    fun beforeSubmit()
    fun blurAndHideSoftInput()
    val hasError: Boolean
}
