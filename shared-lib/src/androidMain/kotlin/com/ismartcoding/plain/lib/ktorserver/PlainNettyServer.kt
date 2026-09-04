/*
 * Plain single-engine Netty server. Collapses the former
 * ApplicationEngine/EmbeddedServer abstraction into one class: it builds the
 * Application, wires the default send/receive transformations, and serves
 * requests through a single user-supplied handler — no call pipeline, no
 * routing tree, no plugin system.
 */

package com.ismartcoding.plain.lib.ktorserver

import com.ismartcoding.plain.lib.ktorserver.core.application.Application
import com.ismartcoding.plain.lib.ktorserver.core.application.PipelineCall
import com.ismartcoding.plain.lib.ktorserver.core.engine.DefaultUncaughtExceptionHandler
import com.ismartcoding.plain.lib.ktorserver.core.engine.EngineConnectorConfig
import com.ismartcoding.plain.lib.ktorserver.core.engine.applicationEnvironment
import com.ismartcoding.plain.lib.ktorserver.core.engine.installDefaultTransformations
import com.ismartcoding.plain.lib.ktorserver.core.engine.BaseApplicationResponse
import com.ismartcoding.plain.lib.ktorserver.core.engine.withPort
import io.ktor.util.network.port
import com.ismartcoding.plain.lib.ktorserver.core.response.ApplicationSendPipeline
import com.ismartcoding.plain.lib.ktorserver.core.application.ApplicationEnvironment
import com.ismartcoding.plain.lib.ktorserver.events.Events
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelOption
import io.netty.channel.ChannelPipeline
import io.netty.channel.EventLoopGroup
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.kqueue.KQueue
import io.netty.channel.kqueue.KQueueServerSocketChannel
import io.netty.channel.socket.ServerSocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.HttpObjectDecoder
import io.netty.handler.codec.http.HttpServerCodec
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.system.measureTimeMillis

/**
 * A Netty-based HTTP(S) server driven by a single [requestHandler].
 *
 * Connectors (plain HTTP and/or SSL) are registered in the configure block via
 * the [connector][com.ismartcoding.plain.lib.ktorserver.core.engine.connector]
 * and [sslConnector][com.ismartcoding.plain.lib.ktorserver.core.engine.sslConnector]
 * builders.
 *
 * @param requestHandler invoked for every request; must produce exactly one
 *   response (the engine commits an empty response if none was sent)
 */
public class PlainNettyServer(
    public val requestHandler: suspend (PipelineCall) -> Unit,
    configure: Configuration.() -> Unit = {},
) {
    public class Configuration {
        /**
         * The connectors to bind (plain HTTP and/or SSL). Populated via the
         * [connector][com.ismartcoding.plain.lib.ktorserver.core.engine.connector]
         * and [sslConnector][com.ismartcoding.plain.lib.ktorserver.core.engine.sslConnector]
         * builders.
         */
        public val connectors: MutableList<EngineConnectorConfig> = mutableListOf()

        /**
         * Number of concurrently running requests from the same http pipeline
         */
        public var runningLimit: Int = 32

        /**
         * Do not create separate call event group and reuse worker group for processing calls
         */
        public var shareWorkGroup: Boolean = false

        /**
         * If set to `true`, enables TCP keep alive for connections so all
         * dead client connections will be discarded.
         */
        public var tcpKeepAlive: Boolean = false

        /**
         * Timeout in seconds for sending responses to client
         */
        public var responseWriteTimeoutSeconds: Int = 10

        /**
         * Timeout in seconds for reading requests from client, "0" is infinite.
         */
        public var requestReadTimeoutSeconds: Int = 0

        /**
         * Thread group sizing mirrors the former Ktor Netty defaults: the call
         * group gets one thread per processor because route handlers run there.
         */
        public var connectionGroupSize: Int = Runtime.getRuntime().availableProcessors() / 2 + 1

        public var workerGroupSize: Int = Runtime.getRuntime().availableProcessors() / 2 + 1

        public var callGroupSize: Int = Runtime.getRuntime().availableProcessors()

        /**
         * User-provided function to configure Netty's [ServerBootstrap]
         */
        public var configureBootstrap: ServerBootstrap.() -> Unit = {}

        /**
         * User-provided function to configure Netty's [HttpServerCodec]
         */
        public var httpServerCodec: () -> HttpServerCodec = {
            HttpServerCodec(
                io.netty.handler.codec.http.HttpObjectDecoder.DEFAULT_MAX_INITIAL_LINE_LENGTH,
                io.netty.handler.codec.http.HttpObjectDecoder.DEFAULT_MAX_HEADER_SIZE,
                io.netty.handler.codec.http.HttpObjectDecoder.DEFAULT_MAX_CHUNK_SIZE
            )
        }

        /**
         * User-provided function to configure Netty's [ChannelPipeline]
         */
        public var channelPipelineConfig: ChannelPipeline.() -> Unit = {}

        public var shutdownGracePeriod: Long = 1000

        public var shutdownTimeout: Long = 2000

        public var log: org.slf4j.Logger = org.slf4j.LoggerFactory.getLogger("plain.netty")
    }

    private val configuration: Configuration = Configuration().apply(configure)

    public val environment: ApplicationEnvironment = applicationEnvironment {
        log = configuration.log
    }

    /**
     * The Application hosting the shared send/receive pipelines and log.
     */
    public val application: Application = Application(
        environment,
        developmentMode = false,
        rootPath = "",
        monitor = Events(),
        parentCoroutineContext = kotlin.coroutines.EmptyCoroutineContext,
        engineProvider = { error("PlainNettyServer has no engine abstraction") }
    )

    private val connectionEventGroup: EventLoopGroup by lazy {
        EventLoopGroupProxy.create(configuration.connectionGroupSize)
    }

    private val workerEventGroup: EventLoopGroup by lazy {
        if (configuration.shareWorkGroup) {
            connectionEventGroup
        } else {
            EventLoopGroupProxy.create(configuration.workerGroupSize)
        }
    }

    private val callEventGroup: EventLoopGroup by lazy {
        if (configuration.shareWorkGroup) {
            workerEventGroup
        } else {
            EventLoopGroupProxy.create(configuration.callGroupSize)
        }
    }

    private val workerDispatcher by lazy {
        workerEventGroup.asCoroutineDispatcher()
    }

    private var channels: List<Channel>? = null

    private val resolvedConnectors = CompletableDeferred<List<EngineConnectorConfig>>()

    private val bootstraps: List<ServerBootstrap> by lazy {
        configuration.connectors.map(::createBootstrap)
    }

    init {
        BaseApplicationResponse.setupSendPipeline(application.sendPipeline)
        application.receivePipeline.installDefaultTransformations()
        application.sendPipeline.installDefaultTransformations()
    }

    private fun createBootstrap(connector: EngineConnectorConfig): ServerBootstrap {
        return ServerBootstrap().apply(configuration.configureBootstrap).apply {
            if (config().group() == null && config().childGroup() == null) {
                group(connectionEventGroup, workerEventGroup)
            }

            if (config().channelFactory() == null) {
                channel(getChannelClass().java)
            }

            val userContext =
                NettyApplicationCallHandler.CallHandlerCoroutineName +
                    NettyDispatcher +
                    DefaultUncaughtExceptionHandler(environment.log)

            childHandler(
                NettyChannelInitializer(
                    { application },
                    requestHandler,
                    environment,
                    callEventGroup,
                    workerDispatcher,
                    userContext,
                    connector,
                    configuration.runningLimit,
                    configuration.responseWriteTimeoutSeconds,
                    configuration.requestReadTimeoutSeconds,
                    configuration.httpServerCodec,
                    configuration.channelPipelineConfig,
                )
            )
            // Send small responses immediately instead of waiting for Nagle
            childOption(ChannelOption.TCP_NODELAY, true)
            if (configuration.tcpKeepAlive) {
                childOption(ChannelOption.SO_KEEPALIVE, true)
            }
        }
    }

    /**
     * Starts the server. When [wait] is true this call blocks until the
     * server stops; otherwise it returns immediately after binding.
     */
    public fun start(wait: Boolean = false): PlainNettyServer {
        try {
            channels = bootstraps.zip(configuration.connectors)
                .map { it.first.bind(it.second.host, it.second.port) }
                .map { it.sync().channel() }
            val resolved = channels!!.zip(configuration.connectors)
                .map { it.second.withPort(it.first.localAddress().port) }
            resolvedConnectors.complete(resolved)
        } catch (cause: Throwable) {
            terminate()
            throw cause
        }

        if (wait) {
            channels?.map { it.closeFuture() }?.forEach { it.sync() }
            stop(configuration.shutdownGracePeriod, configuration.shutdownTimeout)
        }
        return this
    }

    /**
     * The actually bound connectors — use to read ephemeral ports (port = 0).
     */
    public suspend fun resolvedConnectors(): List<EngineConnectorConfig> {
        return resolvedConnectors.await()
    }

    public fun stop(gracePeriodMillis: Long, timeoutMillis: Long) {
        val channelsCloseTime = measureTimeMillis {
            val channelFutures = channels?.mapNotNull { if (it.isOpen) it.close() else null }.orEmpty()
            channelFutures.forEach { future ->
                withStopException { future.sync() }
            }
        }

        // No quiet period: Netty's EventLoopGroup accepts new tasks during the
        // grace period, which would stall shutdown.
        val noQuietPeriod = 0L

        var remainingTimeoutMillis = (timeoutMillis - channelsCloseTime).coerceAtLeast(100L)

        val connectionsShutdownTime = measureTimeMillis {
            withStopException {
                connectionEventGroup.shutdownGracefully(
                    noQuietPeriod,
                    remainingTimeoutMillis,
                    TimeUnit.MILLISECONDS
                ).sync()
            }
        }

        remainingTimeoutMillis = (remainingTimeoutMillis - connectionsShutdownTime).coerceAtLeast(100L)

        val workersShutdownTime = measureTimeMillis {
            withStopException {
                workerEventGroup.shutdownGracefully(
                    gracePeriodMillis.coerceAtMost(remainingTimeoutMillis),
                    remainingTimeoutMillis,
                    TimeUnit.MILLISECONDS
                ).sync()
            }
        }

        if (!configuration.shareWorkGroup) {
            withStopException {
                remainingTimeoutMillis = (remainingTimeoutMillis - workersShutdownTime).coerceAtLeast(100L)
                callEventGroup.shutdownGracefully(noQuietPeriod, remainingTimeoutMillis, TimeUnit.MILLISECONDS).sync()
            }
        }
    }

    private fun terminate() {
        withStopException {
            if (connectionEventGroup !== workerEventGroup) {
                connectionEventGroup.shutdownGracefully().sync()
            }
        }
        withStopException {
            workerEventGroup.shutdownGracefully().sync()
        }
    }

    private inline fun <R> withStopException(crossinline block: () -> R) {
        runCatching(block).onFailure {
            environment.log.error("Exception thrown during server stop", it)
        }
    }

    public override fun toString(): String {
        return "PlainNettyServer(${configuration.connectors})"
    }
}

internal fun getChannelClass(): KClass<out ServerSocketChannel> = when {
    KQueue.isAvailable() -> KQueueServerSocketChannel::class
    Epoll.isAvailable() -> EpollServerSocketChannel::class
    else -> NioServerSocketChannel::class
}
