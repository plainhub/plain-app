/*
 * Copyright 2014-2026 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

/**
 * Vendored from io.ktor:ktor-server-netty:3.5.2.
 */

package com.ismartcoding.plain.lib.ktorserver

import com.ismartcoding.plain.lib.ktorserver.core.application.*
import com.ismartcoding.plain.lib.ktorserver.core.engine.*
import com.ismartcoding.plain.lib.ktorserver.core.http.*
import com.ismartcoding.plain.lib.ktorserver.core.logging.mdcProvider
import com.ismartcoding.plain.lib.ktorserver.core.logging.toLogString
import com.ismartcoding.plain.lib.ktorserver.core.plugins.BadRequestException
import com.ismartcoding.plain.lib.ktorserver.core.plugins.CannotTransformContentToTypeException
import com.ismartcoding.plain.lib.ktorserver.core.plugins.NotFoundException
import com.ismartcoding.plain.lib.ktorserver.core.plugins.PayloadTooLargeException
import com.ismartcoding.plain.lib.ktorserver.core.plugins.UnsupportedMediaTypeException
import com.ismartcoding.plain.lib.ktorserver.core.request.*
import com.ismartcoding.plain.lib.ktorserver.core.response.*
import io.ktor.http.*
import io.ktor.util.cio.*
import io.ktor.util.pipeline.*
import io.ktor.utils.io.*
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.*
import io.netty.handler.timeout.ReadTimeoutException
import io.netty.util.concurrent.EventExecutorGroup
import kotlinx.coroutines.*
import kotlinx.io.IOException
import java.nio.channels.ClosedChannelException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeoutException
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException

internal class NettyHttp1Handler(
    private val applicationProvider: () -> Application,
    private val requestHandler: suspend (PipelineCall) -> Unit,
    private val environment: ApplicationEnvironment,
    private val callEventGroup: EventExecutorGroup,
    private val engineContext: CoroutineContext,
    private val userContext: CoroutineContext,
    private val runningLimit: Int
) : ChannelInboundHandlerAdapter() {
    private val handlerJob = CompletableDeferred<Nothing>()

    private var skipEmpty = false

    private lateinit var responseWriter: NettyHttpResponsePipeline

    private val state = NettyHttpHandlerState(runningLimit)

    private val activeCalls = ConcurrentLinkedQueue<NettyHttp1ApplicationCall>()

    private var activated = false

    // Per-channel cache of the connection-stable portion of the per-call coroutine context.
    // The dispatcher, user context, application context, and coroutine name are reused across
    // all requests on this connection, so we build them once and combine only with the per-call
    // [Job] on each request.
    private var channelApplication: Application? = null
    private var channelCoroutineContext: CoroutineContext = EmptyCoroutineContext

    override fun channelActive(context: ChannelHandlerContext) {
        // channelActive may be fired more than once on this handler (for example, when the pipeline is
        // reconfigured during an HTTP/2 cleartext upgrade or via an explicit fireChannelActive call
        // after adding the handler). Guard against re-adding the body handler and the tail sink, which
        // must be present exactly once per pipeline.
        if (activated) {
            context.fireChannelActive()
            return
        }
        activated = true

        responseWriter = NettyHttpResponsePipeline(
            context = context,
            httpHandlerState = state,
            coroutineContext = handlerJob
        )

        context.channel().config().isAutoRead = false
        context.channel().read()
        context.pipeline().apply {
            addLast(RequestBodyHandler(context))
            // Append a tail sink that consumes NettyHttp1ApplicationCall messages forwarded by this handler
            // via fireChannelRead(call). This prevents Netty's tail handler from logging
            // "Discarded inbound message" warnings for calls that pass through any user-added
            // channelPipelineConfig handlers. The call lifecycle is driven by handleRequest, so the sink
            // only needs to drop the call without further action.
            addLast(NettyHttp1ApplicationCallSink)
        }
        context.fireChannelActive()
    }

    override fun channelRead(context: ChannelHandlerContext, message: Any) {
        if (message is LastHttpContent) {
            state.isCurrentRequestFullyRead.compareAndSet(expect = false, update = true)
        }

        when (message) {
            is HttpRequest -> {
                if (message !is LastHttpContent) {
                    state.isCurrentRequestFullyRead.compareAndSet(expect = true, update = false)
                }
                state.isChannelReadCompleted.compareAndSet(expect = true, update = false)
                state.activeRequests.incrementAndGet()

                handleRequest(context, message)
                callReadIfNeeded(context)
            }

            is LastHttpContent if !message.content().isReadable && skipEmpty -> {
                skipEmpty = false
                message.release()
                callReadIfNeeded(context)
            }

            else -> {
                context.fireChannelRead(message)
            }
        }
    }

    override fun channelInactive(context: ChannelHandlerContext) {
        onConnectionClose(context)
        context.fireChannelInactive()
    }

    private fun onConnectionClose(context: ChannelHandlerContext) {
        if (context.channel().isActive) {
            return
        }
        activeCalls.clear()
    }

    @Suppress("OverridingDeprecatedMember")
    override fun exceptionCaught(context: ChannelHandlerContext, cause: Throwable) {
        when (cause) {
            is IOException -> {
                environment.log.trace("I/O operation failed", cause)
                handlerJob.cancel()
                context.close()
            }

            is ReadTimeoutException -> {
                if (activeCalls.isEmpty()) {
                    context.fireExceptionCaught(cause)
                    return
                }
                context.respond408RequestTimeoutHttp1()
                activeCalls.forEach { call ->
                    call.coroutineContext.cancel(CancellationException(cause))
                }
            }

            else -> {
                handlerJob.completeExceptionally(cause)
                context.close()
            }
        }
    }

    override fun channelReadComplete(context: ChannelHandlerContext?) {
        state.isChannelReadCompleted.compareAndSet(expect = false, update = true)
        responseWriter.flushIfNeeded()
        super.channelReadComplete(context)
    }

    private fun handleRequest(context: ChannelHandlerContext, message: HttpRequest) {
        val callExecutor = pinnedCallExecutor(context, callEventGroup)
        val application = applicationProvider()
        // Building the coroutine context is quite expensive, so we cache most of the elements.
        val baseContext = when {
            application === channelApplication && channelCoroutineContext !== EmptyCoroutineContext ->
                channelCoroutineContext

            else -> {
                val newContext = application.coroutineContext +
                    userContext +
                    NettyDispatcher.CurrentContext(context, callExecutor) +
                    NettyApplicationCallHandler.CallHandlerCoroutineName
                channelApplication = application
                channelCoroutineContext = newContext
                newContext
            }
        }
        val callJob = Job(parent = baseContext[Job])

        // Only the per-call [Job] is combined per request; the rest of the context is cached on the
        // handler instance and reused across all calls on this connection.
        val callContext = baseContext + callJob
        val call = prepareCallFromRequest(context, message, callContext = callContext)
        activeCalls.add(call)

        // Fire channel read for custom handlers added to the pipeline
        context.fireChannelRead(call)

        // Reserve response slot synchronously on the I/O thread for proper ordering
        responseWriter.processResponse(call)

        // Defer coroutine start to the next event loop tick so that channelReadComplete() fires first.
        // This allows the response pipeline to detect that the request body is still being received and flush headers
        // early instead of buffering them, which is required when the client waits for response headers
        // before sending the request body.
        // Dispatching to the call event group also ensures user handler code does not run on the I/O worker
        // event loop.
        callExecutor.execute {
            val callScope = CoroutineScope(context = callContext)
            callScope.launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    if (!call.request.isValid()) {
                        call.respondError400BadRequest()
                        return@launch
                    }
                    try {
                        requestHandler(call)
                    } catch (error: ChannelIOException) {
                        call.application.mdcProvider.withMDCBlock(call) {
                            logFailure(call, error)
                        }
                    } catch (error: Throwable) {
                        handleFailure(call, error)
                    } finally {
                        // [NettyApplicationCall.finish] is non-suspending: it only ensures the
                        // response is committed (headers + status flushed). The actual write
                        // completion is awaited via structured concurrency — the call's
                        // responseWriteJob is a child of the call's coroutine Job.
                        (call as? NettyApplicationCall)?.finish()
                        try {
                            val version = HttpProtocolVersion.parse(call.request.httpVersion)
                            if (version.major == 1) {
                                // In HTTP/1.1, we should read the entire request body to reuse
                                // the persistent connection.
                                call.request.receiveChannel().discard()
                            }
                        } catch (_: Throwable) {
                        }
                    }
                } finally {
                    activeCalls.remove(call)
                    callJob.complete()
                }
            }
        }
    }

    /**
     * Returns netty application call with [message] as a request
     * and channel for request body
     */
    private fun prepareCallFromRequest(
        context: ChannelHandlerContext,
        message: HttpRequest,
        callContext: CoroutineContext
    ): NettyHttp1ApplicationCall {
        val requestBodyChannel = when {
            message is LastHttpContent && !message.content().isReadable -> null

            message.method() === io.netty.handler.codec.http.HttpMethod.GET &&
                !HttpUtil.isContentLengthSet(message) &&
                !HttpUtil.isTransferEncodingChunked(message) -> {
                skipEmpty = true
                null
            }

            else -> prepareRequestContentChannel(context, message)
        }
        return NettyHttp1ApplicationCall(
            application = applicationProvider(),
            context = context,
            httpRequest = message,
            requestBodyChannel = requestBodyChannel,
            engineContext = engineContext,
            coroutineContext = callContext
        )
    }

    private fun prepareRequestContentChannel(
        context: ChannelHandlerContext,
        message: HttpRequest
    ): ByteReadChannel {
        val bodyHandler = context.pipeline().get(RequestBodyHandler::class.java)
        val result = bodyHandler.newChannel()

        if (message is HttpContent) {
            bodyHandler.channelRead(context, message)
        }

        return result
    }

    private fun callReadIfNeeded(context: ChannelHandlerContext) {
        if (state.activeRequests.value < runningLimit) {
            context.read()
            state.skippedRead.value = false
        } else {
            state.skippedRead.value = true
        }
    }
}

/**
 * A no-op tail handler that swallows [NettyHttp1ApplicationCall] messages forwarded by
 * [NettyHttp1Handler.handleRequest] via [ChannelHandlerContext.fireChannelRead]. Its sole purpose is to
 * prevent Netty's default tail handler from logging a "Discarded inbound message ... at the tail of the
 * pipeline" warning. Non-call messages are propagated unchanged so they can reach Netty's tail and be
 * released/handled normally.
 */
@ChannelHandler.Sharable
internal object NettyHttp1ApplicationCallSink : ChannelInboundHandlerAdapter() {
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is NettyHttp1ApplicationCall) return
        ctx.fireChannelRead(msg)
    }
}

/**
 * Logs the [error] and responds with an appropriate error status code.
 */
internal suspend fun handleFailure(call: NettyHttp1ApplicationCall, error: Throwable) {
    try {
        logFailure(call, error)
    } catch (_: OutOfMemoryError) {
    }
    val statusCode = defaultExceptionStatusCode(error) ?: HttpStatusCode.InternalServerError
    tryRespondError(call, statusCode, error.message)
}

private fun defaultExceptionStatusCode(cause: Throwable): HttpStatusCode? = when (cause) {
    is BadRequestException -> HttpStatusCode.BadRequest
    is NotFoundException -> HttpStatusCode.NotFound
    is UnsupportedMediaTypeException,
    is CannotTransformContentToTypeException -> HttpStatusCode.UnsupportedMediaType
    is PayloadTooLargeException -> HttpStatusCode.PayloadTooLarge
    is TimeoutException, is TimeoutCancellationException -> HttpStatusCode.GatewayTimeout
    else -> null
}

private suspend fun tryRespondError(call: NettyHttp1ApplicationCall, statusCode: HttpStatusCode, message: String?) {
    if (call.response.isCommitted || call.response.isSent) return
    try {
        when (message) {
            null -> call.respond(statusCode)
            else -> call.respond(statusCode, message)
        }
    } catch (_: BaseApplicationResponse.ResponseAlreadySentException) {
    }
}

private fun logFailure(call: NettyHttp1ApplicationCall, cause: Throwable) {
    try {
        val status = call.response.status() ?: "Unhandled"
        val logString = try {
            call.request.toLogString()
        } catch (logCause: Throwable) {
            "(request error: $logCause)"
        }

        val infoString = "$status: $logString. Exception ${cause::class}: ${cause.message}"
        when (cause) {
            is CancellationException,
            is ClosedChannelException,
            is ChannelIOException,
            is IOException,
            is BadRequestException,
            is NotFoundException,
            is PayloadTooLargeException,
            is UnsupportedMediaTypeException,
            is CannotTransformContentToTypeException -> call.application.environment.log.debug(infoString, cause)

            else -> call.application.environment.log.error(infoString, cause)
        }
    } catch (_: OutOfMemoryError) {
    }
}
