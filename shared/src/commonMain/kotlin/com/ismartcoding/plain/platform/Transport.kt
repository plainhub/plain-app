package com.ismartcoding.plain.platform

import com.ismartcoding.plain.chat.peer.transport.PeerTransport

expect val isWifiAwareSupported: Boolean

expect fun createWifiAwareTransport(): PeerTransport?
