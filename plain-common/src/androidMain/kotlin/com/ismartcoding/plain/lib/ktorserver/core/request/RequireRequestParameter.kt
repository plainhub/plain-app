/*
 * Copyright 2014-2025 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

/**
 * Vendored from io.ktor:ktor-server-core:3.5.2.
 */

package com.ismartcoding.plain.lib.ktorserver.core.request

import io.ktor.http.*
import com.ismartcoding.plain.lib.ktorserver.core.application.*
import com.ismartcoding.plain.lib.ktorserver.core.plugins.*
import com.ismartcoding.plain.lib.ktorserver.core.routing.*

/**
 * Get query parameter value associated with [name] or fail with [MissingRequestParameterException]
 *
 *
 * @throws MissingRequestParameterException if no query parameter with [name] is present
 */
public fun ApplicationCall.requireQueryParameter(name: String): String {
    return request.queryParameters[name] ?: throw MissingRequestParameterException(name, "query")
}

/**
 * Get header value associated with [name] or fail with [MissingRequestParameterException]
 *
 *
 * @throws MissingRequestParameterException if no header with [name] is present
 */
public fun ApplicationCall.requireHeader(name: String): String {
    return request.headers[name] ?: throw MissingRequestParameterException(name, "header")
}

/**
 * Get cookie value associated with [name] or fail with [MissingRequestParameterException]
 *
 *
 * @throws MissingRequestParameterException if no cookie with [name] is present
 */
public fun ApplicationCall.requireCookie(
    name: String,
    encoding: CookieEncoding = CookieEncoding.URI_ENCODING
): String {
    return request.cookies[name, encoding] ?: throw MissingRequestParameterException(name, "cookie")
}

/**
 * Get path parameter value associated with [name] or fail with [MissingRequestParameterException]
 *
 *
 * @throws MissingRequestParameterException if no path parameter with [name] is present
 */
public fun RoutingCall.requirePathParameter(name: String): String {
    return pathParameters[name] ?: throw MissingRequestParameterException(name, "path")
}
