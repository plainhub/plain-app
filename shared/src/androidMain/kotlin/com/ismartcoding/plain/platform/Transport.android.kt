package com.ismartcoding.plain.platform

import com.ismartcoding.plain.chat.peer.transport.PeerTransport
import com.ismartcoding.plain.chat.peer.transport.WifiAwareTransport
import com.ismartcoding.plain.wifiAwareManager

actual val isWifiAwareSupported: Boolean
    get() = isTPlus() && WifiAwareTransport.isSupported()

actual fun createWifiAwareTransport(): PeerTransport? =
    if (isWifiAwareSupported) WifiAwareTransport else null

actual fun getAwareDataInterfaces(): Int? =
    if (isTPlus()) runCatching { wifiAwareManager.getCharacteristics()?.getNumberOfSupportedDataInterfaces() }.getOrNull() else null

actual fun getAwareDataPaths(): Int? =
    if (isTPlus()) runCatching { wifiAwareManager.getCharacteristics()?.getNumberOfSupportedDataPaths() }.getOrNull() else null
