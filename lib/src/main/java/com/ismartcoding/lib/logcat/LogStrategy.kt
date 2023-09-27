package com.ismartcoding.lib.logcat

interface LogStrategy {
    /**
     * This is invoked by Logger each time a log message is processed.
     * Interpret this method as last destination of the log in whole pipeline.
     *
     * @param priority is the log level e.g. DEBUG, WARNING
     * @param tag is the given tag for the log message.
     * @param message is the given message for the log message.
     */
    fun log(
        priority: Int,
        tag: String?,
        message: String,
    )
}
