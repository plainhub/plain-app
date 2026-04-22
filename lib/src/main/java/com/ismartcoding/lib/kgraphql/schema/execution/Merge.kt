package com.ismartcoding.lib.kgraphql.schema.execution

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

fun MutableMap<String, JsonElement>.merge(key: String, node: JsonElement?): MutableMap<String, JsonElement> {
    mergeEntry(key, node, this::get) { k, v -> this[k] = v }
    return this
}

private fun mergeEntry(
    key: String,
    node: JsonElement?,
    get: (String) -> JsonElement?,
    set: (String, JsonElement) -> Unit
) {
    val safeNode = node ?: JsonNull
    val existingNode = get(key)
    if (existingNode != null) {
        when {
            safeNode is JsonNull -> throw IllegalStateException("trying to merge null with non-null for $key")
            safeNode is JsonObject && existingNode is JsonObject -> {
                set(key, mergeObjects(existingNode, safeNode))
            }
            safeNode is JsonObject -> throw IllegalStateException("trying to merge object with simple node for $key")
            existingNode is JsonObject -> throw IllegalStateException("trying to merge simple node with object node for $key")
            safeNode != existingNode -> throw IllegalStateException("trying to merge different simple nodes for $key")
        }
    } else {
        set(key, safeNode)
    }
}

private fun mergeObjects(existing: JsonObject, incoming: JsonObject): JsonObject {
    val result = existing.toMutableMap()
    incoming.forEach { (k, v) ->
        mergeEntry(k, v, result::get) { key, value -> result[key] = value }
    }
    return JsonObject(result)
}
