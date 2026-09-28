/*
* Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
*/

/**
 * Vendored from io.ktor:ktor-server-core:3.5.2.
 */

package com.ismartcoding.plain.lib.ktorserver.core.response

import io.ktor.http.*
import io.ktor.http.content.*
import com.ismartcoding.plain.lib.ktorserver.core.application.*
import com.ismartcoding.plain.lib.ktorserver.core.util.*
import io.ktor.utils.io.*

/**
 * An HTTP/2 push builder.
 *
 *
 * @property method HTTP method
 * @property url builder
 * @property headers builder
 */
@InternalAPI
@UseHttp2Push
public class DefaultResponsePushBuilder(
    override var method: HttpMethod = HttpMethod.Get,
    override val url: URLBuilder = URLBuilder(),
    override val headers: HeadersBuilder = HeadersBuilder(),
    versions: List<Version> = emptyList()
) : ResponsePushBuilder {

    public constructor(url: URLBuilder, headers: Headers) : this(
        url = url,
        headers = HeadersBuilder().apply { appendAll(headers) }
    )

    public constructor(call: ApplicationCall) : this(
        url = URLBuilder.createFromCall(call),
        headers = HeadersBuilder().apply {
            appendAll(call.request.headers)
            set(HttpHeaders.Referrer, call.url())
        }
    )

    /**
     * A list of version information (for conditional headers).
     *
     */
    override var versions: ArrayList<Version> =
        if (versions.isEmpty()) ArrayList() else ArrayList(versions)
}
