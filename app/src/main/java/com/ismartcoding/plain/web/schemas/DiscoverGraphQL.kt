package com.ismartcoding.plain.web.schemas

import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.discover.NearbyDiscoverManager
import com.ismartcoding.plain.events.StartNearbyDiscoveryEvent
import com.ismartcoding.plain.events.StopNearbyDiscoveryEvent

fun SchemaBuilder.addDiscoverSchema() {
    mutation("startDiscovery") {
        resolver { ->
            sendEvent(StartNearbyDiscoveryEvent())
            true
        }
    }
    mutation("stopDiscovery") {
        resolver { ->
            sendEvent(StopNearbyDiscoveryEvent())
            true
        }
    }
    query("isDiscovering") {
        resolver { ->
            NearbyDiscoverManager.isDiscovering()
        }
    }
}