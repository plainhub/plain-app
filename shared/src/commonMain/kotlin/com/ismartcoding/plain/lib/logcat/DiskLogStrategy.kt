package com.ismartcoding.plain.lib.logcat

expect class DiskLogStrategy() : LogStrategy {
    override fun log(priority: Int, tag: String?, message: String)
}