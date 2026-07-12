package com.ismartcoding.plain.platform

import com.ismartcoding.plain.chat.peer.transport.PeerTransport

actual val isWifiAwareSupported: Boolean = false

actual fun createWifiAwareTransport(): PeerTransport? = null
