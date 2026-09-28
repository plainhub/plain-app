/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

/**
 * Vendored from io.ktor:ktor-server-core:3.5.2.
 */

package com.ismartcoding.plain.lib.ktorserver.core.http

import io.ktor.http.*
import com.ismartcoding.plain.lib.ktorserver.core.response.*

/**
 * Append `Link` header to HTTP response
 *
 */
public fun ApplicationResponse.link(header: LinkHeader): Unit = headers.append(HttpHeaders.Link, header.toString())

/**
 * Append `Link` header to HTTP response with specified [uri] and [rel]
 *
 */
public fun ApplicationResponse.link(uri: String, vararg rel: String): Unit = link(LinkHeader(uri, *rel))
