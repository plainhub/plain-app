/*
* Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
*/

/**
 * Vendored from io.ktor:ktor-server-netty:3.5.2.
 */

package com.ismartcoding.plain.lib.ktorserver

import com.ismartcoding.plain.lib.ktorserver.core.application.*
import com.ismartcoding.plain.lib.ktorserver.core.engine.*
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelPipeline
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.HttpServerExpectContinueHandler
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.SslHandler
import io.netty.handler.timeout.ReadTimeoutException
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import io.netty.util.concurrent.EventExecutorGroup
import java.io.FileInputStream
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.net.ssl.TrustManagerFactory
import kotlin.coroutines.CoroutineContext

/**
 * A [ChannelInitializer] implementation that sets up the default ktor channel pipeline
 *
 */
public class NettyChannelInitializer(
    private val applicationProvider: () -> Application,
    private val requestHandler: suspend (PipelineCall) -> Unit,
    private val environment: ApplicationEnvironment,
    private val callEventGroup: EventExecutorGroup,
    private val engineContext: CoroutineContext,
    private val userContext: CoroutineContext,
    private val connector: EngineConnectorConfig,
    private val runningLimit: Int,
    private val responseWriteTimeout: Int,
    private val requestReadTimeout: Int,
    private val httpServerCodec: () -> HttpServerCodec,
    private val channelPipelineConfig: ChannelPipeline.() -> Unit
) : ChannelInitializer<SocketChannel>() {
    private var sslContext: SslContext? = null

    init {
        if (connector is EngineSSLConnectorConfig) {
            @Suppress("UNCHECKED_CAST")
            val chain1 = connector.keyStore.getCertificateChain(connector.keyAlias).toList() as List<X509Certificate>
            val certs = chain1.toList().toTypedArray()
            val password = connector.privateKeyPassword()
            val pk = connector.keyStore.getKey(connector.keyAlias, password) as PrivateKey
            password.fill('\u0000')

            sslContext = SslContextBuilder.forServer(pk, *certs)
                .apply {
                    connector.trustManagerFactory()?.let { this.trustManager(it) }
                }
                .build()
        }
    }

    override fun initChannel(ch: SocketChannel) {
        with(ch.pipeline()) {
            if (connector is EngineSSLConnectorConfig) {
                val sslEngine = sslContext!!.newEngine(ch.alloc()).apply {
                    if (connector.hasTrustStore()) {
                        useClientMode = false
                        needClientAuth = true
                    }
                    connector.enabledProtocols?.let {
                        enabledProtocols = it.toTypedArray()
                    }
                }
                addLast("ssl", SslHandler(sslEngine))
            }
            configurePipeline(this)
        }
    }

    private fun configurePipeline(pipeline: ChannelPipeline) {
        val handler = NettyHttp1Handler(
            applicationProvider,
            requestHandler,
            environment,
            callEventGroup,
            engineContext,
            userContext,
            runningLimit
        )

        with(pipeline) {
            if (requestReadTimeout > 0) {
                addLast("readTimeout", KtorReadTimeoutHandler(requestReadTimeout))
            }
            addLast("codec", httpServerCodec())
            addLast("continue", HttpServerExpectContinueHandler())
            addLast("timeout", WriteTimeoutHandler(responseWriteTimeout))
            addLast("http1", handler)
            channelPipelineConfig()
        }

        pipeline.context("codec").fireChannelActive()
    }

    private fun EngineSSLConnectorConfig.hasTrustStore() = trustStore != null || trustStorePath != null

    private fun EngineSSLConnectorConfig.trustManagerFactory(): TrustManagerFactory? {
        val trustStore = trustStore ?: trustStorePath?.let { file ->
            FileInputStream(file).use { fis ->
                KeyStore.getInstance(KeyStore.getDefaultType()).also { it.load(fis, null) }
            }
        }
        return trustStore?.let { store ->
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).also { it.init(store) }
        }
    }
}

internal class KtorReadTimeoutHandler(requestReadTimeout: Int) : ReadTimeoutHandler(requestReadTimeout) {
    private var closed = false

    override fun readTimedOut(ctx: ChannelHandlerContext?) {
        if (!closed) {
            ctx?.fireExceptionCaught(ReadTimeoutException.INSTANCE)
            closed = true
        }
    }
}
