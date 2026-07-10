package com.ismartcoding.plain.discover

import com.ismartcoding.plain.data.DPairingSession

object PairingSessionStore {
    private val sessions = mutableMapOf<String, DPairingSession>()

    fun put(session: DPairingSession) = synchronized(sessions) {
        sessions[session.deviceId] = session
    }

    fun get(deviceId: String): DPairingSession? = synchronized(sessions) {
        sessions[deviceId]
    }

    fun remove(deviceId: String) = synchronized(sessions) {
        sessions.remove(deviceId)
    }
}
