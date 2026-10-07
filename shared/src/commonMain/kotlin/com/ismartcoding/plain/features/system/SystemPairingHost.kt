package com.ismartcoding.plain.features.system

import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.json.*

internal object SystemPairingHost {
    suspend fun handle(method: String, params: JsonObject): JsonElement = when (method) {
        "systemPeerFacts" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.chat.peer.PeerCacher.peersMap.value.values
            .map { it.peer })
        "systemDeletePeer" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.chat.peer.PeerManager.deletePeer(
                params.getValue("id").jsonPrimitive.content))
        "systemUnpairPeer" -> {
            com.ismartcoding.plain.ui.models.NearbyViewModel.unpairDevice(
                params.getValue("id").jsonPrimitive.content)
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemPairDevice" -> {
            val input = params.getValue("input").jsonObject
            com.ismartcoding.plain.ui.models.NearbyViewModel.startPairing(nearbyDevice(input))
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemCancelPairing" -> {
            com.ismartcoding.plain.ui.models.NearbyViewModel.cancelPairing(
                params.getValue("deviceId").jsonPrimitive.content)
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemRespondToPairing" -> {
            com.ismartcoding.plain.discover.PairingResponder.respond(
                pairingRequest(params.getValue("input").jsonObject),
                params.getValue("accepted").jsonPrimitive.boolean)
            JsonHelper.jsonEncodeToElement(true)
        }
        else -> error("Unsupported provider operation")
    }

    private fun nearbyDevice(input: JsonObject): com.ismartcoding.plain.data.DNearbyDevice =
        JsonHelper.jsonDecodeFromElement(input)

    private fun pairingRequest(input: JsonObject): com.ismartcoding.plain.data.DPairingRequest =
        JsonHelper.jsonDecodeFromElement(input)
}
