package com.ismartcoding.plain.platform

expect fun discoverPeerDevice(peerId: String, key: ByteArray)

expect fun canShowNotifications(): Boolean
