package com.ismartcoding.plain.features.chat.peer

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.models.PeerRecordInput
import com.ismartcoding.plain.rust.graphql.GraphqlApi

object RustPeerStore {
    suspend fun getAll(): List<Peer> = GraphqlApi.execute(Query.peers())

    suspend fun getById(id: String): Peer? = getAll().find { it.id == id }

    suspend fun remove(id: String): Boolean = GraphqlApi.execute(Mutation.deletePeer(id))

    suspend fun unpair(id: String): Boolean = GraphqlApi.execute(Mutation.unpairPeer(id))

    suspend fun fileUrl(peer: Peer, fileId: String): String {
        return GraphqlApi.execute(Query.peerFileUrl(peer.id, PeerRecordInput.from(peer), fileId))
    }
}
