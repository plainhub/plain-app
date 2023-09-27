package com.ismartcoding.lib.logcat

/**
 * <pre>
 * ┌────────────────────────────────────────────
 * │ LOGGER
 * ├┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄
 * │ Standard logging mechanism
 * ├┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄
 * │ But more pretty, simple and powerful
 * └────────────────────────────────────────────
</pre> *
 *
 * <h3>How to use it</h3>
 * Initialize it first
 * <pre>`
 * Logger.addLogAdapter(new AndroidLogAdapter());
`</pre> *
 *
 * And use the appropriate static Logger methods.
 *
 * <pre>`
 * Logger.d("debug");
 * Logger.e("error");
 * Logger.w("warning");
 * Logger.v("verbose");
 * Logger.i("information");
 * Logger.wtf("What a Terrible Failure");
`</pre> *
 *
 * <h3>String format arguments are supported</h3>
 * <pre>`
 * Logger.d("hello %s", "world");
`</pre> *
 *
 * <h3>Collections are support ed(only available for debug logs)</h3>
 * <pre>`
 * Logger.d(MAP);
 * Logger.d(SET);
 * Logger.d(LIST);
 * Logger.d(ARRAY);
`</pre> *
 *
 * <h3>Json and Xml support (output will be in debug level)</h3>
 * <pre>`
 * Logger.json(JSON_CONTENT);
 * Logger.xml(XML_CONTENT);
`</pre> *
 *
 * <h3>Customize Logger</h3>
 * Based on your needs, you can change the following settings:
 *
 *  * Different [LogAdapter]
 *  * Different [FormatStrategy]
 *  * Different [LogStrategy]
 *
 *
 * @see LogAdapter
 *
 * @see FormatStrategy
 *
 * @see LogStrategy
 */
object LogCat {
    const val VERBOSE = 2
    const val DEBUG = 3
    const val INFO = 4
    const val WARN = 5
    const val ERROR = 6
    const val ASSERT = 7
    private var printer = LoggerPrinter()

    fun addLogAdapter(adapter: LogAdapter) {
        printer.addAdapter(adapter)
    }

    fun clearLogAdapters() {
        printer.clearLogAdapters()
    }

    /**
     * Given tag will be used as tag only once for this method call regardless of the tag that's been
     * set during initialization. After this invocation, the general tag that's been set will
     * be used for the subsequent log calls
     */
    fun t(tag: String?): Printer {
        return printer.t(tag)
    }

    /**
     * General log function that accepts all configurations as parameter
     */
    fun log(
        priority: Int,
        tag: String?,
        message: String?,
        throwable: Throwable?,
    ) {
        printer.log(priority, tag, message, throwable)
    }

    fun d(
        message: String,
        vararg args: Any?,
    ) {
        printer.d(message, args)
    }

    fun d(`object`: Any?) {
        printer.d(`object`)
    }

    fun e(
        message: String,
        vararg args: Any?,
    ) {
        printer.e(null, message, args)
    }

    fun e(
        throwable: Throwable?,
        message: String = "",
        vararg args: Any?,
    ) {
        printer.e(throwable, message, args)
    }

    fun i(
        message: String,
        vararg args: Any?,
    ) {
        printer.i(message, args)
    }

    fun v(
        message: String,
        vararg args: Any?,
    ) {
        printer.v(message, args)
    }

    fun w(
        message: String,
        vararg args: Any?,
    ) {
        printer.w(message, args)
    }

    /**
     * Tip: Use this for exceptional situations to log
     * ie: Unexpected errors etc
     */
    fun wtf(
        message: String,
        vararg args: Any?,
    ) {
        printer.wtf(message, args)
    }

    /**
     * Formats the given json content and print it
     */
    fun json(json: String?) {
        printer.json(json)
    }

    /**
     * Formats the given xml content and print it
     */
    fun xml(xml: String?) {
        printer.xml(xml)
    }

    init {
        addLogAdapter(AndroidLogAdapter())
    }
}
