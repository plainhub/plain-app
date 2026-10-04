package com.ismartcoding.plain.httpserver.mainschemas

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.discover.RustMdnsRuntime
import kotlinx.serialization.json.*

@GraphQLMutation
suspend fun startDiscovery(): Boolean {
    RustMdnsRuntime.control("start")
    return true
}

@GraphQLMutation
suspend fun stopDiscovery(): Boolean {
    RustMdnsRuntime.control("stop")
    return true
}

@GraphQLQuery
suspend fun isDiscovering(): Boolean {
    return RustMdnsRuntime.snapshot().getValue("scanning").jsonPrimitive.boolean
}

fun SchemaBuilder.addDiscoverSchema() {
}
