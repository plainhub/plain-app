package com.ismartcoding.lib.logcat

/**
 * Android terminal log output implementation for [LogAdapter].
 *
 * Prints output to LogCat with pretty borders.
 *
 * <pre>
 * ┌──────────────────────────
 * │ Method stack history
 * ├┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄
 * │ Log message
 * └──────────────────────────
</pre> *
 */
class AndroidLogAdapter : LogAdapter {
    private val formatStrategy: FormatStrategy

    constructor() {
        formatStrategy = PrettyFormatStrategy.newBuilder().build()
    }

    constructor(formatStrategy: FormatStrategy) {
        this.formatStrategy = formatStrategy
    }

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
