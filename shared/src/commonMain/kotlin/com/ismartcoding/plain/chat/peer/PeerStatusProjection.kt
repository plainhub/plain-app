package com.ismartcoding.plain.chat.peer

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*

object PeerStatusProjection {
    private val lock = Mutex()

    suspend fun refresh(payload: String? = null) = lock.withLock {
        val row = RustContentApi.postJsonOrThrow("chat/peer-status", JsonHelper.jsonEncodeToElement(PeerStatusRequest("snapshot")).jsonObject).getValue("result").jsonObject
        val event = payload?.let { JsonHelper.jsonDecode<JsonElement>(it).jsonObject }
        if (event != null && event["runtimeId"] != row["runtimeId"]) return@withLock
        val online = row.getValue("online").jsonArray.map { it.jsonPrimitive.content }.toSet()
        PeerStatusManager.applySnapshot(online)
    }
}
