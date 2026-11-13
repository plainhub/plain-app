package com.ismartcoding.plain.features.nearby

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.models.NearbyDevice
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.ui.nearby.NearbyViewModel
import com.ismartcoding.plain.rust.graphql.AppSubscriptions
import com.ismartcoding.plain.rust.graphql.changes
import kotlinx.coroutines.flow.map

object RustNearbyDevices {
    suspend fun observeDiscovery() {
        discoverySession(
            start = {
                GraphqlApi.execute(Mutation.startDiscovery())
                apply(snapshot())
                RustMdnsRuntime.refresh()
            },
            stop = { GraphqlApi.execute(Mutation.stopDiscovery()) },
            scanning = AppSubscriptions.mdnsChanged.changes().map { RustMdnsRuntime.snapshot().scanning },
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
