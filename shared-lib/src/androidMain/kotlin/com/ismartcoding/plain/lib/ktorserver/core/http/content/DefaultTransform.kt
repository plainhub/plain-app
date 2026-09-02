/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

/**
 * Vendored from io.ktor:ktor-server-core:3.5.2.
 */

package com.ismartcoding.plain.lib.ktorserver.core.http.content

import io.ktor.http.*
import io.ktor.http.content.*
import com.ismartcoding.plain.lib.ktorserver.core.application.*
import com.ismartcoding.plain.lib.ktorserver.core.response.*
import io.ktor.utils.io.*

/**
 * Default outgoing content transformation
 *
 */
public fun transformDefaultContent(call: ApplicationCall, value: Any): OutgoingContent? = when (value) {
    is OutgoingContent -> value

    is String -> {
        val contentType = call.defaultTextContentType(null)
        TextContent(value, contentType, null)
    }

    is ByteArray -> {
        ByteArrayContent(value)
    }

    is HttpStatusCode -> {
        HttpStatusCodeContent(value)
    }

    is ByteReadChannel -> object : OutgoingContent.ReadChannelContent() {
        override fun readFrom(): ByteReadChannel = value
    }

    else -> platformTransformDefaultContent(call, value)
}

