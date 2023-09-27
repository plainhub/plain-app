package com.ismartcoding.lib.logcat

class DiskLogAdapter(private val formatStrategy: FormatStrategy) : LogAdapter {
    override fun isLoggable(
        priority: Int,
        tag: String?,
    ): Boolean {
        return true
    }

    override fun log(
        priority: Int,
        tag: String?,
        message: String,
    ) {
        formatStrategy.log(priority, tag, message)
    }
}
