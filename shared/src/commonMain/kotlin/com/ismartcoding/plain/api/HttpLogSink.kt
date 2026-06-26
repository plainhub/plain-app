package com.ismartcoding.plain.api

fun interface HttpLogSink {
    fun log(message: String)
}

var httpLogSink: HttpLogSink = HttpLogSink { println("[HttpLog] $it") }