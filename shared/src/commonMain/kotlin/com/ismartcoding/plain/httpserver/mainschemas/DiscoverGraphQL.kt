package com.ismartcoding.plain.httpserver.mainschemas

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.discover.MdnsDiscoverManager
import com.ismartcoding.plain.ui.models.NearbyViewModel

@GraphQLMutation
suspend fun startDiscovery(): Boolean {
    NearbyViewModel.startDiscovering()
    return true
}

@GraphQLMutation
suspend fun stopDiscovery(): Boolean {
    NearbyViewModel.stopDiscovering()
    return true
}

@GraphQLQuery
suspend fun isDiscovering(): Boolean {
    return MdnsDiscoverManager.isDiscovering()
}

fun SchemaBuilder.addDiscoverSchema() {
}
