package com.ismartcoding.plain.features.nearby

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.models.NearbyDevice
import com.ismartcoding.plain.models.PairPeerInput
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.rust.native.RustHostReports

object RustPairingRuntime {
    suspend fun start(device: NearbyDevice, mode: PairingMode = PairingMode.CHAT): Boolean =
        GraphqlApi.execute(Mutation.pairPeer(PairPeerInput(peerId = device.id, mode = mode)))

    suspend fun submit(peerId: String, mode: PairingMode, password: String): Boolean =
        GraphqlApi.execute(Mutation.pairPeer(PairPeerInput(peerId, mode, password)))

    suspend fun cancel(peerId: String): Boolean = GraphqlApi.execute(Mutation.cancelPairing(peerId))

    suspend fun complete(reply: String) { RustHostReports.pairingReply(reply) }

    suspend fun respond(peerId: String, accepted: Boolean): Boolean =
        GraphqlApi.execute(Mutation.respondToPairing(peerId, accepted))
}
