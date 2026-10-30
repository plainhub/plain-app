package com.ismartcoding.plain.graphql

import com.ismartcoding.plain.features.dlna.sender.CastSnapshot
import com.ismartcoding.plain.rust.graphql.GraphqlRequest

object Subscription {
    internal fun dlnaSenderChanged(): GraphqlRequest<CastSnapshot> = GraphqlRequest(
        "subscription { dlnaSenderChanged { ${Fragments.DLNA_STATE} } }",
        field = "dlnaSenderChanged",
    )
}
