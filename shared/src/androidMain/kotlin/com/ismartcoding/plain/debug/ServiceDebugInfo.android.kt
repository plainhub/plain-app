package com.ismartcoding.plain.debug

import com.ismartcoding.plain.chat.peer.transport.WifiAwareTransport
import com.ismartcoding.plain.lib.mdns.MdnsHostResponder
import com.ismartcoding.plain.services.HttpServerService

actual fun isHttpServerRunning(): Boolean = HttpServerService.isRunning()

actual fun isMdnsRunning(): Boolean = MdnsHostResponder.isRunning

actual fun getAwareAttachStatus(): String =
    if (WifiAwareTransport.awareSession != null) "attached" else "not attached"

actual fun getAwareDiscoveredPeerCount(): Int = WifiAwareTransport.discoveredPeerCount
