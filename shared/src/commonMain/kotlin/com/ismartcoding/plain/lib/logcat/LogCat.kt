package com.ismartcoding.plain.lib.logcat

expect object LogCat {
    val VERBOSE: Int
    val DEBUG: Int
    val INFO: Int
    val WARN: Int
    val ERROR: Int
    val ASSERT: Int

    fun d(message: Any?, vararg args: Any?)
    fun e(message: Any?, vararg args: Any?)
    fun i(message: Any?, vararg args: Any?)
    fun w(message: Any?, vararg args: Any?)
    fun v(message: Any?, vararg args: Any?)
    fun wtf(message: Any?, vararg args: Any?)

    fun addLogAdapter(adapter: LogAdapter)
    fun clearLogAdapters()

    fun init(context: Any?)

    fun logFolder(): String
}