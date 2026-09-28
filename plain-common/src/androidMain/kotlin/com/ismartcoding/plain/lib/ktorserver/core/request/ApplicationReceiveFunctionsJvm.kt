/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

/**
 * Vendored from io.ktor:ktor-server-core:3.5.2.
 */

package com.ismartcoding.plain.lib.ktorserver.core.request

import com.ismartcoding.plain.lib.ktorserver.core.application.*
import java.io.*

/**
 * Receives stream content for this call.
 *
 *
 * @return instance of [InputStream] to read incoming bytes for this call.
 * @throws ContentTransformationException when content cannot be transformed to the [InputStream].
 */
@Suppress("NOTHING_TO_INLINE")
public suspend inline fun ApplicationCall.receiveStream(): InputStream = receive()

@PublishedApi
internal val DEFAULT_FORM_FIELD_LIMIT: Long
    get() = System.getProperty("io.ktor.server.request.formFieldLimit")?.toLongOrNull() ?: (50 * 1024 * 1024L)
