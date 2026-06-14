package com.ismartcoding.plain.web.schemas

import com.ismartcoding.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.chat.PeerManager
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
            PeerManager.deletePeer(MainApp.instance, id.value)
            true
        }
    }
    type<Peer> {}
}
