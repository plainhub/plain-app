package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.discover.PairingResponder
import com.ismartcoding.plain.ui.models.NearbyViewModel
import com.ismartcoding.plain.web.models.PairingDeviceInput
import com.ismartcoding.plain.web.models.PairingRequestInput

fun SchemaBuilder.addPairingSchema() {
    mutation("pairDevice") {
        description = "Initiate pairing with a discovered LAN device."
        resolver("input") { input: PairingDeviceInput ->
            NearbyViewModel.startPairing(input.toModel())
            true
        }
    }

    mutation("cancelPairing") {
        description = "Cancel an in-progress pairing initiated by this device."
        resolver("deviceId") { deviceId: String ->
            NearbyViewModel.cancelPairing(deviceId)
            true
        }
    }

    mutation("respondToPairing") {
        description = "Respond to an incoming pairing request — accept or reject."
        resolver("input", "accepted") { input: PairingRequestInput, accepted: Boolean ->
            PairingResponder.respond(input.toModel(), accepted)
            true
        }
    }
}
