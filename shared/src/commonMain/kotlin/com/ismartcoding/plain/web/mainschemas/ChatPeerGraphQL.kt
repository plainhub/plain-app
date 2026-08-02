package com.ismartcoding.plain.web.mainschemas

import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.chat.peer.PeerManager
import com.ismartcoding.plain.ui.models.NearbyViewModel
import com.ismartcoding.plain.web.models.ID
import com.ismartcoding.plain.web.models.Peer
import com.ismartcoding.plain.web.models.toModel

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
    // Peer type is registered via @GraphQLType + registerGeneratedSchema()
}
