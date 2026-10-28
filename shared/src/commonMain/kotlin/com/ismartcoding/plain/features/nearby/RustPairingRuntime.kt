package com.ismartcoding.plain.features.nearby

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.models.NearbyDevice
import com.ismartcoding.plain.models.PairingDeviceInput
import com.ismartcoding.plain.models.PairingRequestInput
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.rust.native.RustHostReports
import com.ismartcoding.plain.rust.native.RustNativeApi

object RustPairingRuntime {
    suspend fun states(): List<PairingState> = GraphqlApi.execute(Query.pairingStates())

    suspend fun start(device: NearbyDevice, mode: PairingMode = PairingMode.CHAT): PairingState {
        val ip = if (DiscoveryMethod.QR in device.discoveryMethods) device.bestIp.takeIf { it.isNotBlank() } ?: device.ips.firstOrNull() else null
        val host = ip?.let { "${if (':' in it) "[$it]" else it}:${device.port}" }
        val state = GraphqlApi.execute(
            Mutation.pairDevice(PairingDeviceInput(id = device.id, host = host, mode = mode)),
        )
        PairingProjection.reconcile()
        return state
    }

    suspend fun submit(
        generation: String,
        password: String,
    ): PairingState =
        GraphqlApi.execute(Mutation.pairDevice(PairingDeviceInput(generation = generation, password = password)))

    suspend fun cancelGeneration(generation: String): Boolean =
        GraphqlApi.execute(Mutation.cancelPairing(generation))

    suspend fun cancel(id: String): Boolean {
        val attempt = states().lastOrNull {
            it.deviceId == id && it.phase !in setOf(PairingPhase.SUCCEEDED, PairingPhase.FAILED, PairingPhase.CANCELLED)
        } ?: return false
        return cancelGeneration(attempt.generation)
    }

    suspend fun currentRequest(id: String, signature: String): Boolean =
        RustNativeApi.isPairingRequestCurrent(id, signature)

    suspend fun complete(payload: String) {
        RustHostReports.pairingResponse(payload)
    }

    suspend fun respond(request: PairingRequestInput, accepted: Boolean): Boolean =
        GraphqlApi.execute(Mutation.respondToPairing(request, accepted))
}
