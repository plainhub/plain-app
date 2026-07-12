package com.ismartcoding.plain.ble.server

import com.ismartcoding.plain.platform.PlatformLock

object BlePairingSessionStore {
    private val macs = mutableMapOf<String, String>()
    private val lock = PlatformLock()

    fun put(fromId: String, clientMac: String) {
        lock.withLock { macs[fromId] = clientMac }
    }

    fun get(fromId: String): String? = lock.withLock { macs[fromId] }

    fun has(fromId: String): Boolean = lock.withLock { macs.containsKey(fromId) }

    fun remove(fromId: String) {
        lock.withLock { macs.remove(fromId) }
    }
}
