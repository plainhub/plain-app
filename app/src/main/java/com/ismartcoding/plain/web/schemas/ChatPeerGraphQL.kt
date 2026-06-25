package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.chat.peer.PeerManager
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.web.models.ID
import com.ismartcoding.plain.web.models.Peer
import com.ismartcoding.plain.web.models.toModel

fun SchemaBuilder.addPeerSchema() {
    query("peers") {
        resolver { ->
            AppDatabase.instance.peerDao().getAll().map { it.toModel() }
        }
    }
    mutation("deletePeer") {
        resolver { id: ID ->
            PeerManager.deletePeer(id.value)
            true
        }
    }

    mutation("unpairPeer") {
        resolver { id: ID ->
            PeerManager.markUnpaired(id.value)
            true
        }
    }
    type<Peer> {}
}
