package com.ismartcoding.plain.debug

import com.ismartcoding.plain.mdns.MdnsHostResponder
import com.ismartcoding.plain.platform.IosPlatformRegistry

actual fun isHttpServerRunning(): Boolean =
    IosPlatformRegistry.httpServerBridge()?.isRunning() == true

actual fun isMdnsRunning(): Boolean = MdnsHostResponder.isRunning

actual fun getAwareAttachStatus(): String = "not available"
actual fun getAwareDiscoveredPeerCount(): Int = 0
