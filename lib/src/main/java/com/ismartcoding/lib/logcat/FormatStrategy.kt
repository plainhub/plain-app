package com.ismartcoding.lib.logcat

interface FormatStrategy {
    fun log(
        priority: Int,
        tag: String?,
        message: String,
    )
}
