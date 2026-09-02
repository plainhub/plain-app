/*
 * Copyright 2014-2025 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

/**
 * Vendored from io.ktor:ktor-server-core:3.5.2.
 */

package com.ismartcoding.plain.lib.ktorserver.core.engine.internal

import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import com.ismartcoding.plain.lib.ktorserver.core.application.ApplicationCall
import com.ismartcoding.plain.lib.ktorserver.core.plugins.origin
import com.ismartcoding.plain.lib.ktorserver.core.request.httpMethod
import com.ismartcoding.plain.lib.ktorserver.core.request.path
import io.ktor.utils.io.ByteReadChannel

internal class ExceptionPageContent(call: ApplicationCall, cause: Throwable) : OutgoingContent.ReadChannelContent() {

    override val status: HttpStatusCode
        get() = HttpStatusCode.InternalServerError

    private val responsePage: String = buildString {
        val request = call.request
        append("<html><body><h1>Internal Server Error</h1><h2>Request Information:</h2><pre>")
        append("Method: ${request.httpMethod}\n")
        append("Path: ${request.path()}\n")
        append("Parameters: ${request.rawQueryParameters}\n")
        append("From origin: ${request.origin}\n")
        append("</pre><h2>Stack Trace:</h2><pre>")

        val stackTrace = cause.stackTraceToString().lines()
        stackTrace.forEach { element ->
            append("<span style=\"color:blue;\">$element</span><br>")
        }
        var currentCause = cause.cause
        while (currentCause != null) {
            append("<br>Caused by:<br>")
            val causeStack = currentCause.stackTraceToString().lines()
            causeStack.forEach { element ->
                append("<span style=\"color:green;\">$element</span><br>")
            }
            currentCause = currentCause.cause
        }
        append("</pre></body></html>")
    }

    override fun readFrom(): ByteReadChannel = ByteReadChannel(responsePage)
}
