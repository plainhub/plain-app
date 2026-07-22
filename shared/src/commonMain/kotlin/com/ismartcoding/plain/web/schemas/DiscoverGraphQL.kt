package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.discover.LANDiscoverManager
import com.ismartcoding.plain.ui.models.NearbyViewModel

fun SchemaBuilder.addDiscoverSchema() {
    mutation("startDiscovery") {
        resolver { ->
            NearbyViewModel.startDiscovering()
            true
        }
    }
    mutation("stopDiscovery") {
        resolver { ->
            NearbyViewModel.stopDiscovering()
            true
        }
    }
    query("isDiscovering") {
        resolver { ->
            LANDiscoverManager.isDiscovering()
        }
    }
}
