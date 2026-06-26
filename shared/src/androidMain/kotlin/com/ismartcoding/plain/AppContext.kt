package com.ismartcoding.plain

import android.content.Context

@PublishedApi
internal var appContextValue: Context? = null

fun setAppContext(context: Context) {
    appContextValue = context
}

val appContext: Context
    get() = appContextValue ?: error("setAppContext must be called before appContext is used")
