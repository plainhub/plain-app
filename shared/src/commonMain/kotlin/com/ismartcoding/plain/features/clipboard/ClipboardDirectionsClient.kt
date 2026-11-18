package com.ismartcoding.plain.features.clipboard

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.preferences.PrefKeys
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal enum class ClipboardDirection { OFF, SEND, RECEIVE, BOTH }

internal data class ClipboardDevice(val id: String, val name: String, val direction: ClipboardDirection)

internal class ClipboardDirectionsClient(private val api: GraphqlClient = GraphqlApi) {
    private val updates = Mutex()
    private val key = PrefKeys.CLIPBOARD_DIRECTIONS

    suspend fun devices(): List<ClipboardDevice> = updates.withLock {
        val directions = directions()
        managedPeers().map { ClipboardDevice(it.id, it.name, direction(directions[it.id])) }
    }

    suspend fun setDirection(peerId: String, direction: ClipboardDirection): ClipboardDirection = updates.withLock {
        check(managedPeers().any { it.id == peerId }) { "Peer is not managed" }
        val updated = JsonObject(directions() + (peerId to JsonPrimitive(direction.name)))
        check(api.execute(Mutation.setPref(key, updated))) { "Clipboard direction was not saved" }
        direction(directions()[peerId])
    }

    private suspend fun managedPeers() = api.execute(Query.peerSummaries()).filter { it.isPaired() && it.managed }

    private suspend fun directions(): JsonObject {
        val value = api.execute(Query.prefs(listOf(key)))[key] ?: return JsonObject(emptyMap())
        check(value is JsonObject) { "Invalid clipboard directions preference" }
        return value
    }

    private fun direction(value: JsonElement?): ClipboardDirection =
        if (value == null) ClipboardDirection.RECEIVE
        else ClipboardDirection.entries.firstOrNull { JsonPrimitive(it.name) == value } ?: ClipboardDirection.OFF
}
