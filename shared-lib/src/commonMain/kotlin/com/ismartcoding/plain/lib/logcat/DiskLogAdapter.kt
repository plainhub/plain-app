package com.ismartcoding.plain.lib.logcat

class DiskLogAdapter(
    private val formatStrategy: FormatStrategy,
    private val minPriority: Int = LogCat.VERBOSE,
) : LogAdapter {
    override fun isLoggable(priority: Int, tag: String?): Boolean {
        return priority >= minPriority
    }

    override fun log(priority: Int, tag: String?, message: String) {
        formatStrategy.log(priority, tag, message)
    }
}