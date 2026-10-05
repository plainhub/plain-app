package com.ismartcoding.plain.chat.peer

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.chat.peer.transport.PeerTransportType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*

object PeerTransportProjection {
    const val EVENT_UPDATED = 10003
    private val lock = Mutex()

    suspend fun refresh() = lock.withLock {
        val active = RustContentApi.postJson("chat/transport", buildJsonObject { put("action", "snapshotTransfers") }).getValue("result").jsonObject
        PeerCacher.currentTransportMap.value = active.mapValues { (_, kind) -> PeerTransportType.valueOf(kind.jsonPrimitive.content) }
    }
}
