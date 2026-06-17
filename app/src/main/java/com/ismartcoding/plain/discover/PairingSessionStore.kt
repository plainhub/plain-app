package com.ismartcoding.plain.discover

import com.ismartcoding.plain.data.DPairingSession
import java.util.concurrent.ConcurrentHashMap

object PairingSessionStore {
    private val sessions = ConcurrentHashMap<String, DPairingSession>()

    fun put(session: DPairingSession) {
        sessions[session.deviceId] = session
    }

    fun get(deviceId: String): DPairingSession? = sessions[deviceId]

    fun remove(deviceId: String) {
        sessions.remove(deviceId)
    }
}
