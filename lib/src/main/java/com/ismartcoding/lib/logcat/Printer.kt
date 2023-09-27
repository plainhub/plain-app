package com.ismartcoding.lib.logcat

/**
 * A proxy interface to enable additional operations.
 * Contains all possible Log message usages.
 */
interface Printer {
    fun addAdapter(adapter: LogAdapter)

    fun t(tag: String?): Printer?

    fun d(
        message: String,
        vararg args: Any?,
    )

    fun d(`object`: Any?)

    fun e(
        message: String,
        vararg args: Any?,
    )

    fun e(
        throwable: Throwable?,
        message: String,
        vararg args: Any?,
    )

    fun w(
        message: String,
        vararg args: Any?,
    )

    fun i(
        message: String,
        vararg args: Any?,
    )

    fun v(
        message: String,
        vararg args: Any?,
    )

    fun wtf(
        message: String,
        vararg args: Any?,
    )

    /**
     * Formats the given json content and print it
     */
    fun json(json: String?)

    /**
     * Formats the given xml content and print it
     */
    fun xml(xml: String?)

    fun log(
        priority: Int,
        tag: String?,
        message: String?,
        throwable: Throwable?,
    )

    fun clearLogAdapters()
}
