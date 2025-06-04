package com.ismartcoding.plain.data

import android.content.Context

interface ISelectOption {
    suspend fun isSelected(context: Context): Boolean

    fun getText(): String
}

