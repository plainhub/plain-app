package com.ismartcoding.plain.web.mainschemas

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.discover.PairingResponder
import com.ismartcoding.plain.ui.models.NearbyViewModel
import com.ismartcoding.plain.web.models.PairingDeviceInput
import com.ismartcoding.plain.web.models.PairingRequestInput

@GraphQLMutation(description = "Initiate pairing with a discovered LAN device.")
suspend fun pairDevice(input: PairingDeviceInput): Boolean {
    NearbyViewModel.startPairing(input.toModel())
    return true
}

@GraphQLMutation(description = "Cancel an in-progress pairing initiated by this device.")
suspend fun cancelPairing(deviceId: String): Boolean {
    NearbyViewModel.cancelPairing(deviceId)
    return true
}

@GraphQLMutation(description = "Respond to an incoming pairing request — accept or reject.")
suspend fun respondToPairing(input: PairingRequestInput, accepted: Boolean): Boolean {
    PairingResponder.respond(input.toModel(), accepted)
    return true
}

fun SchemaBuilder.addPairingSchema() {
}
