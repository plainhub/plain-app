package com.ismartcoding.plain.features.nearby

import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.models.NearbyDevice
import com.ismartcoding.plain.rust.graphql.GraphqlApi

object RustPairingCode {
    suspend fun parse(code: String): NearbyDevice? =
        GraphqlApi.execute(Query.parsePairingCode(code))
}
