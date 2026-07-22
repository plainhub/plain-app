package com.ismartcoding.plain.discover

import com.ismartcoding.plain.data.DPairingSession
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.PlatformLock

object PairingSessionStore {
    private val sessions = mutableMapOf<String, DPairingSession>()
    private val lock = PlatformLock()

    fun put(session: DPairingSession) = lock.withLock {
        sessions[session.deviceId] = session
    }

    fun get(deviceId: String): DPairingSession? = lock.withLock {
        sessions[deviceId]
    }

    fun remove(deviceId: String) = lock.withLock {
        sessions.remove(deviceId)
    }
}
