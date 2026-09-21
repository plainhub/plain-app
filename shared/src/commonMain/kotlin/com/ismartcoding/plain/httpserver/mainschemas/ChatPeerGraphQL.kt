package com.ismartcoding.plain.httpserver.mainschemas

import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.chat.peer.PeerManager
import com.ismartcoding.plain.ui.models.NearbyViewModel
import com.ismartcoding.plain.httpserver.models.ID
import com.ismartcoding.plain.httpserver.models.Peer
import com.ismartcoding.plain.httpserver.models.toModel
import kotlin.reflect.typeOf

@GraphQLQuery
suspend fun peers(): List<Peer> {
    return PeerCacher.peersMap.value.values.map { it.peer.toModel() }
}

@GraphQLMutation
suspend fun deletePeer(id: ID): Boolean {
    PeerManager.deletePeer(id.value)
    return true
}

@GraphQLMutation
suspend fun unpairPeer(id: ID): Boolean {
    NearbyViewModel.unpairDevice(id.value)
    return true
}

fun SchemaBuilder.addPeerSchema() {
    // Peer type is registered via @GraphQLType + registerGeneratedSchema();
    // id is exposed as ID for contract consistency (wire format stays a string).
    type<Peer> {
        property("id", typeOf<ID>(), { it: Peer -> ID(it.id) })
    }
}
