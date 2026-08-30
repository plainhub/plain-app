package com.ismartcoding.plain.chat.peer.transport.aware

import android.net.Network
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.platform.OkHttpPlainClient
import com.ismartcoding.plain.platform.PlainHttpClient
import com.ismartcoding.plain.platform.SharedOkHttpClients
import com.ismartcoding.plain.platform.createCryptoPlainClient
import okhttp3.Dns
import java.net.Inet6Address
import java.net.InetAddress
import java.util.concurrent.TimeUnit

class AwareHttpClientFactory {
    fun build(
        peerId: String,
        network: Network,
        peerIpv6: Inet6Address,
    ): PlainHttpClient {
        val keyBytes = requireNotNull(PeerCacher.getKeyBytes(peerId)) {
            "PeerCacher has no key bytes for peer $peerId"
        }
        return createCryptoPlainClient(
            keyBytes = keyBytes,
            socketFactory = network.socketFactory,
            dns = awareDns(peerIpv6),
            timeoutSeconds = 30,
            connectTimeoutMs = 5_000L,
        )
    }

    fun buildFileDownload(
        network: Network,
        peerIpv6: Inet6Address,
    ): PlainHttpClient {
        val okHttpClient = SharedOkHttpClients.unsafe.newBuilder()
            .socketFactory(network.socketFactory)
            .dns(awareDns(peerIpv6))
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
        return OkHttpPlainClient(okHttpClient)
    }

    companion object {
        const val AWARE_HOST = "plain-aware-peer"

        private fun awareDns(peerIpv6: Inet6Address): Dns = Dns { hostname ->
            if (hostname == AWARE_HOST) listOf<InetAddress>(peerIpv6) else Dns.SYSTEM.lookup(hostname)
        }
    }
}
