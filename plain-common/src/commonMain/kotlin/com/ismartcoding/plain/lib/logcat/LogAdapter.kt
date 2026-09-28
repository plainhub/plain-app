package com.ismartcoding.plain.lib.logcat

interface LogAdapter {
    fun isLoggable(priority: Int, tag: String?): Boolean

    fun log(priority: Int, tag: String?, message: String)
}