package com.ismartcoding.plain.ble.server

object BlePairingSessionStore {
    private val macs = mutableMapOf<String, String>()
    private val lock = Any()

    fun put(fromId: String, clientMac: String) {
        synchronized(lock) { macs[fromId] = clientMac }
    }

    fun get(fromId: String): String? = synchronized(lock) { macs[fromId] }

    fun has(fromId: String): Boolean = synchronized(lock) { macs.containsKey(fromId) }

    fun remove(fromId: String) {
        synchronized(lock) { macs.remove(fromId) }
    }
}
