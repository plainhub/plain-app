/*
* Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
*/

/**
 * Vendored from io.ktor:ktor-server-netty:3.5.2.
 */

package com.ismartcoding.plain.lib.ktorserver

import io.ktor.http.*
import com.ismartcoding.plain.lib.ktorserver.core.application.*
import io.ktor.utils.io.*
import io.netty.channel.*
import io.netty.handler.codec.http.*
import kotlin.coroutines.*

internal class NettyHttp1ApplicationRequest(
    call: PipelineCall,
    coroutineContext: CoroutineContext,
    context: ChannelHandlerContext,
    val httpRequest: HttpRequest,
    requestBodyChannel: ByteReadChannel
) : NettyApplicationRequest(
    call,
    coroutineContext,
    context,
    requestBodyChannel,
    httpRequest.uri(),
    HttpUtil.isKeepAlive(httpRequest)
) {
    override val local = NettyConnectionPoint(httpRequest, context)

    override val engineHeaders: Headers = NettyApplicationRequestHeaders(httpRequest)
}
