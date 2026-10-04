package com.ismartcoding.plain.chat.peer

import com.ismartcoding.plain.chat.peer.transport.PeerTransportRouter
import com.ismartcoding.plain.chat.peer.transport.SignedRequest
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.helpers.Base64Lenient
import kotlinx.serialization.json.*

object PeerGraphQLClient {
    suspend fun startAware(peer: DPeer): GraphQLResponse = send(RustPeerWireStore.startAware(peer.id))
    private suspend fun send(prepared: JsonObject): GraphQLResponse = PeerTransportRouter.send(
        RustPeerStore.decode(prepared.getValue("peer")),
        SignedRequest(prepared.getValue("body").jsonPrimitive.content, prepared.getValue("channelId").jsonPrimitive.content),
        Base64Lenient.decode(prepared.getValue("key").jsonPrimitive.content),
    )
}
