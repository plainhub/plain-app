package com.ismartcoding.plain.features.nearby

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.ui.nearby.NearbyViewModel

object RustNearbyDevices {
    suspend fun observeDiscovery() {
        discoverySession(
            start = {
                GraphqlApi.execute(Mutation.startDiscovery())
                NearbyViewModel.isDiscovering.value = true
            },
            stop = {
                GraphqlApi.execute(Mutation.stopDiscovery())
                NearbyViewModel.isDiscovering.value = false
            },
            poll = {
                val snapshot = GraphqlApi.execute(Query.nearbyDiscovery())
                NearbyViewModel.nearbyDevices.value = snapshot.nearbyDevices
                NearbyViewModel.isDiscovering.value = snapshot.isDiscovering
                snapshot.isDiscovering
            },
        )
    }
}
