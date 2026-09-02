/*
 * Copyright 2014-2026 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

/**
 * Vendored from io.ktor:ktor-server-websockets:3.5.2.
 */


package com.ismartcoding.plain.lib.ktorserver

import com.ismartcoding.plain.lib.ktorserver.core.application.*
import com.ismartcoding.plain.lib.ktorserver.core.response.*
import io.ktor.util.*
import io.ktor.util.logging.*
import io.ktor.utils.io.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

internal val WEBSOCKETS_LOGGER = KtorSimpleLogger("io.ktor.server.websocket.WebSockets")

/**
 * WebSockets support plugin. It is required to be installed first before binding any websocket endpoints
 *
 * ```
 * install(WebSockets)
 *
 * install(Routing) {
 *     webSocket("/ws") {
 *          incoming.consumeForEach { ... }
 *     }
 * }
 * ```
 *
 *
 *
 * @param pingIntervalMillis duration between pings or [PINGER_DISABLED] to disable pings.
 * @param timeoutMillis write/ping timeout after that a connection will be closed.
 * @param maxFrameSize maximum frame that could be received or sent.
 * @param masking whether masking need to be enabled (useful for security).
 * @param extensionsConfig is configuration for WebSocket extensions.
 * @param channelsConfig configuration for the I/O channels.
 */
public class WebSockets private constructor(
    public val pingIntervalMillis: Long,
    public val timeoutMillis: Long,
    public val maxFrameSize: Long,
    public val masking: Boolean,
    public val extensionsConfig: WebSocketExtensionsConfig,
    public val channelsConfig: WebSocketChannelsConfig = WebSocketChannelsConfig.UNLIMITED
) : CoroutineScope {
    private val parent: CompletableJob = Job()

    public constructor(
        pingIntervalMillis: Long,
        timeoutMillis: Long,
        maxFrameSize: Long,
        masking: Boolean
    ) : this(pingIntervalMillis, timeoutMillis, maxFrameSize, masking, WebSocketExtensionsConfig())

    override val coroutineContext: CoroutineContext
        get() = parent

    init {
        require(pingIntervalMillis >= 0)
        require(timeoutMillis >= 0)
        require(maxFrameSize > 0)
    }

    private fun shutdown() {
        parent.complete()
    }

    /**
     * Websockets configuration options
     *
     */
    @KtorDsl
    public class WebSocketOptions {
        internal val extensionsConfig = WebSocketExtensionsConfig()

        @OptIn(InternalAPI::class)
        internal val channelsConfig = WebSocketChannelsConfig()

        /**
         * Duration between pings or [PINGER_DISABLED] to disable pings
         *
         */
        public var pingPeriodMillis: Long = PINGER_DISABLED

        /**
         * write/ping timeout after that a connection will be closed
         *
         */
        public var timeoutMillis: Long = 15_000L

        /**
         * Maximum frame that could be received or sent
         *
         */
        public var maxFrameSize: Long = Long.MAX_VALUE

        /**
         * Whether masking need to be enabled (useful for security)
         *
         */
        public var masking: Boolean = false

        /**
         * Configuration for the incoming and outgoing [Frame] queues.
         * Both queues are unlimited by default, which may lead to OutOfMemoryError under high backpressure.
         * Some engines don't support suspending limited-size incoming channels — check compatibility before using them.
         *
         * Caution: A bounded incoming channel with [ChannelOverflow.CLOSE] will close on overflow,
         * possibly causing exceptions for any frames received afterward.
         *
         * ```kotlin
         * channels {
         *     incoming = unlimited()
         *     outgoing = bounded(capacity = 512, onOverflow = ChannelOverflow.SUSPEND)
         * }
         * ```
         */
        public fun channels(block: WebSocketChannelsConfig.() -> Unit) {
            channelsConfig.apply(block)
        }

        /**
         * Configure WebSocket extensions.
         *
         */
        public fun extensions(block: WebSocketExtensionsConfig.() -> Unit) {
            extensionsConfig.apply(block)
        }
    }

    /**
     * Plugin installation object.
     *
     */
    public companion object Plugin : BaseApplicationPlugin<Application, WebSocketOptions, WebSockets> {
        override val key: AttributeKey<WebSockets> = AttributeKey("WebSockets")

        /**
         * Key for saving configured WebSocket extensions for the specific call.
         *
         */
        public val EXTENSIONS_KEY: AttributeKey<List<WebSocketExtension<*>>> =
            AttributeKey("WebSocket extensions")

        override fun install(pipeline: Application, configure: WebSocketOptions.() -> Unit): WebSockets {
            val config = WebSocketOptions().also(configure)
            with(config) {
                val webSockets = WebSockets(
                    pingPeriodMillis,
                    timeoutMillis,
                    maxFrameSize,
                    masking,
                    extensionsConfig,
                )

                pipeline.monitor.subscribe(ApplicationStopPreparing) {
                    WEBSOCKETS_LOGGER.trace("Shutdown WebSockets due to application stop")
                    webSockets.shutdown()
                }

                pipeline.sendPipeline.intercept(ApplicationSendPipeline.Transform) {
                    if (it !is WebSocketUpgrade) return@intercept
                }

                return webSockets
            }
        }
    }
}
