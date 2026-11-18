package com.ismartcoding.plain.features.clipboard

import com.ismartcoding.plain.features.nearby.PeerStatus
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.platform.system.DeviceType
import com.ismartcoding.plain.preferences.PrefKeys
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import com.ismartcoding.plain.rust.graphql.GraphqlResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

class ClipboardDirectionsClientTest {
    @Test
    fun onlyManagedPeersAppearEvenWhenOfflineAndInvalidValuesFailClosed() = runTest {
        val local = LocalHost()
        assertEquals(listOf(
            ClipboardDevice("offline", "offline", ClipboardDirection.RECEIVE),
            ClipboardDevice("legacy", "legacy", ClipboardDirection.OFF),
        ), local.client.devices())
    }

    @Test
    fun consecutiveWritesRevalidateAndMergeLatestSnapshotWithoutOverwritingOtherDevices() = runTest {
        val local = LocalHost()
        val send = async { local.client.setDirection("offline", ClipboardDirection.SEND) }
        val both = async { local.client.setDirection("legacy", ClipboardDirection.BOTH) }
        assertEquals(ClipboardDirection.SEND, send.await())
        assertEquals(ClipboardDirection.BOTH, both.await())
        assertEquals(
            buildJsonObject {
                put("offline", "SEND")
                put("legacy", "BOTH")
                put("retained", "OFF")
            },
            local.saved,
        )
        local.peers = local.peers.filterNot { it.id == "offline" }
        assertFailsWith<IllegalStateException> { local.client.setDirection("offline", ClipboardDirection.OFF) }
        assertEquals(2, local.writes)
    }

    @Test
    fun rejectedWritesDoNotReplaceConfirmedValues() = runTest {
        val local = LocalHost()
        local.acceptWrites = false
        assertFailsWith<IllegalStateException> { local.client.setDirection("offline", ClipboardDirection.BOTH) }
        assertEquals(ClipboardDirection.RECEIVE, local.client.devices().first().direction)
    }

    private class LocalHost {
        private val key = PrefKeys.CLIPBOARD_DIRECTIONS
        var peers = listOf(peer("offline"), peer("legacy"), peer("chat-only", managed = false), peer("unpaired", status = PeerStatus.UNPAIRED))
        var saved = JsonObject(mapOf("legacy" to JsonPrimitive("push"), "retained" to JsonPrimitive("OFF")))
        var acceptWrites = true
        var writes = 0
        val client = ClipboardDirectionsClient(GraphqlClient("local fixture") { request ->
            val result = buildJsonObject {
                when {
                    "peers {" in request.query -> {
                        assertFalse(" token " in request.query)
                        assertFalse(" key " in request.query)
                        put("peers", JsonHelper.jsonEncodeToElement(peers))
                    }
                    "prefs(" in request.query -> put("prefs", buildJsonObject { put(key, saved) })
                    "setPref(" in request.query -> {
                        assertEquals(JsonPrimitive(key), request.variables["key"])
                        delay(1)
                        if (acceptWrites) {
                            saved = request.variables.getValue("value").jsonObject
                            writes++
                        }
                        put("setPref", acceptWrites)
                    }
                    else -> error("Unexpected request")
                }
            }
            GraphqlResponse(data = result)
        })
    }

    companion object {
        private fun peer(id: String, managed: Boolean = true, status: PeerStatus = PeerStatus.PAIRED): Peer {
            val timestamp = Instant.parse("2026-01-01T00:00:00Z")
            return Peer(id, id, "", status = status, port = null, deviceType = DeviceType.COMPUTER,
                createdAt = timestamp, updatedAt = timestamp, managed = managed)
        }
    }
}
