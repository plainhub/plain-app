package com.ismartcoding.plain.features.system

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.enums.has
import kotlinx.serialization.json.*

internal object SystemTagsHost {
    suspend fun handle(method: String, params: JsonObject): JsonElement = when (method) {
        "systemMediaTagFacts" -> mediaTagFacts(params)
        "systemTagQueryStubs" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.platform.getMediaTagRelationStubs(
                mediaDataType(params), params.getValue("query").jsonPrimitive.content).map { stub ->
                TagQueryStubFacts(
                    key = stub.key,
                    title = stub.title,
                    size = stub.size,
                )
        })
        "systemTagQueryKeys" -> JsonHelper.jsonEncodeToElement(IdsFacts(
            ids = com.ismartcoding.plain.platform.getMediaIds(
                mediaDataType(params), params.getValue("query").jsonPrimitive.content).toList(),
        ))
        else -> error("Unsupported provider operation")
    }

    /** One entry per key that actually has relations; keys with none are
     * omitted and Rust defaults them to an empty list, so an explicit empty
     * row would just be noise on the wire. */
    private suspend fun mediaTagFacts(params: JsonObject): JsonElement {
        val type = mediaDataType(params)
        val keys = params.getValue("keys").jsonArray.map { it.jsonPrimitive.content }.toSet()
        if (keys.isEmpty()) return JsonHelper.jsonEncodeToElement(emptyList<String>())
        val relations = com.ismartcoding.plain.features.TagHelper
        .getTagRelationsByKeys(keys, type).groupBy { it.key }
        val tags = com.ismartcoding.plain.features.TagHelper.getAll(type).associateBy { it.id }
        return JsonHelper.jsonEncodeToElement(keys.mapNotNull { key ->
            val ids = relations[key]?.map { it.tagId } ?: return@mapNotNull null
            if (ids.isEmpty()) return@mapNotNull null
            MediaTagsFacts(
                key = key,
                tags = ids.mapNotNull { tags[it] }.map { tag ->
                    TagFacts(
                        id = tag.id,
                        name = tag.name,
                        count = tag.count,
                    )
                },
            )
        })
    }
}
