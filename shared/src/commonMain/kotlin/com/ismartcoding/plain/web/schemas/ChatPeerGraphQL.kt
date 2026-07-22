package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.chat.peer.PeerManager
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.ui.models.NearbyViewModel
import com.ismartcoding.plain.web.models.ID
import com.ismartcoding.plain.web.models.Peer
import com.ismartcoding.plain.web.models.toModel

fun SchemaBuilder.addPeerSchema() {
    query("peers") {
        resolver { ->
            PeerCacher.peersMap.value.values.map { it.peer.toModel() }
        }
    }
    mutation("deletePeer") {
        resolver("id") { id: ID ->
            PeerManager.deletePeer(id.value)
            true
        }
    }

    mutation("unpairPeer") {
        resolver("id") { id: ID ->
            NearbyViewModel.unpairDevice(id.value)
            true
        }
    }
    type<Peer> {}
}
