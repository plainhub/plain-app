package com.ismartcoding.plain.chat.peer.transport.aware

import android.net.Network
import okhttp3.OkHttpClient
import java.net.Inet6Address

data class PeerConnection(
    val network: Network,
    val peerIpv6: Inet6Address,
    val peerPort: Int,
    val httpClient: OkHttpClient,
)