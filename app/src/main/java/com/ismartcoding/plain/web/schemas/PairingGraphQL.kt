package com.ismartcoding.plain.web.schemas

import com.ismartcoding.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.discover.NearbyPairManager
import com.ismartcoding.plain.web.models.PairingRequestInput

fun SchemaBuilder.addPairingSchema() {
    mutation("respondToPairing") {
        resolver { input: PairingRequestInput, accepted: Boolean ->
            NearbyPairManager.respondToPairing(input.toModel(), accepted)
            true
        }
    }
}
