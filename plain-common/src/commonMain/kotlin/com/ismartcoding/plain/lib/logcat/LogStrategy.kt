package com.ismartcoding.plain.lib.logcat

interface LogStrategy {
    fun log(priority: Int, tag: String?, message: String)
}