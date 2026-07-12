package com.ismartcoding.plain.chat.peer.transport.aware

import android.net.Network
import com.ismartcoding.plain.api.OkHttpClientFactory
import com.ismartcoding.plain.chat.peer.PeerCacher
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.websocket.WebSockets
import okhttp3.Dns
import java.net.Inet6Address
import java.net.InetAddress
import java.util.concurrent.TimeUnit

class AwareHttpClientFactory {
    fun build(
        peerId: String,
        network: Network,
        peerIpv6: Inet6Address,
        peerPort: Int,
    ): HttpClient {
        val keyBytes = requireNotNull(PeerCacher.getKeyBytes(peerId)) {
            "PeerCacher has no key bytes for peer $peerId"
        }
        val okHttpClient = OkHttpClientFactory.createCryptoHttpClient(
            keyBytes = keyBytes,
            timeout = 30,
            socketFactory = network.socketFactory,
            dns = awareDns(peerIpv6),
            connectTimeoutMs = 5_000L,
            addHeaders = false,
        ).newBuilder()
            .retryOnConnectionFailure(true)
            .build()
        return HttpClient(OkHttp) {
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000L
                connectTimeoutMillis = 5_000L
            }
            install(WebSockets)
            engine {
                preconfigured = okHttpClient
            }
        }
    }

    fun buildFileDownload(
        network: Network,
        peerIpv6: Inet6Address,
    ): HttpClient {
        val okHttpClient = OkHttpClientFactory.createUnsafeOkHttpClient()
            .newBuilder()
            .socketFactory(network.socketFactory)
            .dns(awareDns(peerIpv6))
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
        return HttpClient(OkHttp) {
            install(HttpTimeout) {
                requestTimeoutMillis = 120_000L
                connectTimeoutMillis = 10_000L
            }
            engine {
                preconfigured = okHttpClient
            }
        }
    }

    companion object {
        const val AWARE_HOST = "plain-aware-peer"

        private fun awareDns(peerIpv6: Inet6Address): Dns = Dns { hostname ->
            if (hostname == AWARE_HOST) listOf<InetAddress>(peerIpv6) else Dns.SYSTEM.lookup(hostname)
        }
    }
}
