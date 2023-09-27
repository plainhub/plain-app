package com.ismartcoding.lib.logcat

import android.util.Log

/**
 * LogCat implementation for [LogStrategy]
 *
 * This simply prints out all logs to Logcat by using standard [Log] class.
 */
class LogcatLogStrategy : LogStrategy {
    override fun log(
        priority: Int,
        tag: String?,
        message: String,
    ) {
        var newTag = tag
        if (newTag == null) {
            newTag = DEFAULT_TAG
        }
        Log.println(priority, newTag, message)
    }

    companion object {
        const val DEFAULT_TAG = "NO_TAG"
    }
}
