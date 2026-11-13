package com.ismartcoding.plain.features.nearby

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.models.NearbyDevice
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.ui.nearby.NearbyViewModel

object RustNearbyDevices {
    suspend fun observeDiscovery() {
        apply(snapshot())
        discoverySession(
            start = {
                GraphqlApi.execute(Mutation.startDiscovery())
                NearbyViewModel.isDiscovering.value = true
            },
            stop = {
                GraphqlApi.execute(Mutation.stopDiscovery())
                NearbyViewModel.isDiscovering.value = false
            },
            isDiscovering = {
                GraphqlApi.execute(Query.isDiscovering()).also { NearbyViewModel.isDiscovering.value = it }
            },
        )
    }
    /**
     * Pulls the current cache. Used by the `NEARBY_DEVICES_UPDATED` event
     * handler — the event payload only carries the revision.
     */

    suspend fun snapshot(): List<NearbyDevice> =
        GraphqlApi.execute(Query.nearbyDevices())

    fun apply(devices: List<NearbyDevice>) {
        NearbyViewModel.nearbyDevices.value = devices
    }
}
