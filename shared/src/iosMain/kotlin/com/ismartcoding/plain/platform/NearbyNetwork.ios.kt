package com.ismartcoding.plain.platform

import com.ismartcoding.plain.lib.logcat.LogCat

actual fun nearbySendMulticast(message: String) {
    // iOS does not support UDP multicast LAN discovery yet.
}

actual fun nearbySendUnicast(message: String, targetIP: String) {
    // iOS does not support UDP unicast LAN discovery yet.
}

actual fun nearbyStartReceiver(onMessage: (message: String, senderIP: String) -> Unit) {
    LogCat.d("NearbyNetwork receiver is not supported on iOS")
}

actual fun nearbyStopReceiver() {
}
