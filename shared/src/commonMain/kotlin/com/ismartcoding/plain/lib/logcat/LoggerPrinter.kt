package com.ismartcoding.plain.lib.logcat

internal class LoggerPrinter : Printer {
    private var pendingTag: String? = null
    private val logAdapters: MutableList<LogAdapter> = ArrayList()

    override fun t(tag: String?): Printer {
        if (tag != null) {
            pendingTag = tag
        }
        return this
    }

    override fun d(
        message: String,
        vararg args: Any?,
    ) {
        log(LogCat.DEBUG, null, message, args)
    }

    override fun d(`object`: Any?) {
        log(LogCat.DEBUG, null, `object`?.toString() ?: "null", emptyArray())
    }

    override fun e(
        message: String,
        vararg args: Any?,
    ) {
        e(null, message, *args)
    }

    override fun e(
        throwable: Throwable?,
        message: String?,
        vararg args: Any?,
    ) {
        log(LogCat.ERROR, throwable, message, args)
    }

    override fun w(
        message: String,
        vararg args: Any?,
    ) {
        log(LogCat.WARN, null, message, args)
    }

    override fun i(
        message: String,
        vararg args: Any?,
    ) {
        log(LogCat.INFO, null, message, args)
    }

    override fun v(
        message: String,
        vararg args: Any?,
    ) {
        log(LogCat.VERBOSE, null, message, args)
    }

    override fun wtf(
        message: String,
        vararg args: Any?,
    ) {
        log(LogCat.ASSERT, null, message, args)
    }

    override fun json(json: String?) {}

    override fun xml(xml: String?) {}

    override fun log(
        priority: Int,
        tag: String?,
        message: String?,
        throwable: Throwable?,
    ) {
        var newMessage = message
        if (throwable != null && newMessage != null) {
            newMessage = "$newMessage : ${throwable.stackTraceToString()}"
        }
        if (throwable != null && newMessage == null) {
            newMessage = throwable.stackTraceToString()
        }
        if (newMessage.isNullOrEmpty()) {
            newMessage = "Empty/NULL log message"
        }
        for (adapter in logAdapters) {
            if (adapter.isLoggable(priority, tag)) {
                adapter.log(priority, tag, newMessage)
            }
        }
    }

    override fun clearLogAdapters() {
        logAdapters.clear()
    }

    override fun addAdapter(adapter: LogAdapter) {
        logAdapters.add(adapter)
    }

    private fun log(
        priority: Int,
        throwable: Throwable?,
        msg: String?,
        args: Array<out Any?>,
    ) {
        val formatted = if (args.isEmpty() || msg == null) msg ?: "" else buildString {
            append(msg)
            for (arg in args) {
                append(", ")
                append(arg?.toString() ?: "null")
            }
        }
        val currentTag = pendingTag
        pendingTag = null
        log(priority, currentTag, formatted, throwable)
    }
}