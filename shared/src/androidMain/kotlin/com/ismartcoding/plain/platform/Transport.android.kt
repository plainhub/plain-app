package com.ismartcoding.plain.platform

import com.ismartcoding.plain.chat.peer.transport.PeerTransport
import com.ismartcoding.plain.chat.peer.transport.WifiAwareTransport

actual val isWifiAwareSupported: Boolean
    get() = isTPlus() && WifiAwareTransport.isSupported()

actual fun createWifiAwareTransport(): PeerTransport? =
    if (isWifiAwareSupported) WifiAwareTransport else null
