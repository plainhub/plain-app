package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.discover.LANDiscoverManager
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
            LANDiscoverManager.isDiscovering()
        }
    }
}