package com.ismartcoding.plain.platform

import com.ismartcoding.plain.chat.peer.transport.PeerTransport

expect val isWifiAwareSupported: Boolean

expect fun createWifiAwareTransport(): PeerTransport?

/** Number of supported Wi-Fi Aware data interfaces, or null if unavailable. */
expect fun getAwareDataInterfaces(): Int?

/** Number of supported Wi-Fi Aware data paths, or null if unavailable. */
expect fun getAwareDataPaths(): Int?
