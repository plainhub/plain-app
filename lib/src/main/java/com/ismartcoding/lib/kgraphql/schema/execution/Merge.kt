package com.ismartcoding.lib.kgraphql.schema.execution

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

fun MutableMap<String, JsonNode?>.merge(key: String, node: JsonNode?): MutableMap<String, JsonNode?> {
    merge(key, node, this::get, this::set)
    return this
}

fun ObjectNode.merge(other: ObjectNode) {
    other.fields().forEach {
        merge(it.key, it.value)
    }
}

fun ObjectNode.merge(key: String, node: JsonNode?) {
    merge(key, node, this::get, this::set)
}

fun merge(key: String, node: JsonNode?, get: (String) -> JsonNode?, set: (String, JsonNode?) -> Any?) {
    val existingNode = get(key)
    if (existingNode != null) {
        when {
            node == null -> throw IllegalStateException("trying to merge null with non-null for $key")
            node is ObjectNode -> {
                check(existingNode is ObjectNode) { "trying to merge object with simple node for $key" }
                existingNode.merge(node)
            }
            existingNode is ObjectNode -> throw IllegalStateException("trying to merge simple node with object node for $key")
            node != existingNode -> throw IllegalStateException("trying to merge different simple nodes for $key")
        }
    } else {
        set(key, node)
    }
}
