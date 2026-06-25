package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.discover.NearbyPairing
import com.ismartcoding.plain.web.models.PairingDeviceInput
import com.ismartcoding.plain.web.models.PairingRequestInput

fun SchemaBuilder.addPairingSchema() {
    mutation("pairDevice") {
        description = "Initiate pairing with a discovered LAN device."
        resolver { input: PairingDeviceInput ->
            NearbyPairing.startPairingAsync(input.toModel())
            true
        }
    }

    mutation("cancelPairing") {
        description = "Cancel an in-progress pairing initiated by this device."
        resolver { deviceId: String ->
            NearbyPairing.cancelPairing(deviceId)
            true
        }
    }

    mutation("respondToPairing") {
        description = "Respond to an incoming pairing request — accept or reject."
        resolver { input: PairingRequestInput, accepted: Boolean ->
            NearbyPairing.respondToPairing(input.toModel(), accepted)
            true
        }
    }
}
