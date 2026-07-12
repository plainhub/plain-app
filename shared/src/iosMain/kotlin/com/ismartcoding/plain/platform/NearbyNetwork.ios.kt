package com.ismartcoding.plain.platform

import com.ismartcoding.plain.lib.logcat.LogCat

actual object NearbyNetwork {
    actual fun sendMulticast(message: String) {
        // iOS does not support UDP multicast LAN discovery yet.
    }

    actual fun sendUnicast(message: String, targetIP: String) {
        // iOS does not support UDP unicast LAN discovery yet.
    }

    actual fun startReceiver(onMessage: (message: String, senderIP: String) -> Unit) {
        LogCat.d("NearbyNetwork receiver is not supported on iOS")
    }

    actual fun stopReceiver() {
    }
}
