package com.ismartcoding.plain.api

import com.ismartcoding.plain.db.*
import com.ismartcoding.plain.enums.DataType
import kotlinx.serialization.json.*

internal object RustContentTags {
    fun owns(type: Int): Boolean = type == DataType.NOTE.value || type == DataType.FEED_ENTRY.value
    suspend fun all(type: DataType): List<DTag> = RustContentApi.query("tags(type: ${type.name}) { $TAG_FIELDS }").getValue("tags").jsonArray.map { it.tag() }
    suspend fun get(id: String): DTag? = RustContentApi.query("tag(id: ${gql(id)}) { $TAG_FIELDS }")["tag"]?.takeUnless { it is JsonNull }?.tag()
    suspend fun save(item: DTag, create: Boolean): String {
        val field = if (create) "createTag" else "updateTag"
        val args = if (create) "type: ${DataType.entries.first { it.value == item.type }.name}, name: ${gql(item.name)}" else "id: ${gql(item.id)}, name: ${gql(item.name)}"
        return RustContentApi.mutate("$field($args) { id }").getValue(field).jsonObject.string("id")
    }
    suspend fun delete(id: String) { RustContentApi.mutate("deleteTag(id: ${gql(id)})") }
    suspend fun keys(id: String): List<String> = RustContentApi.query("tagKeys(id: ${gql(id)})").getValue("tagKeys").jsonArray.map { it.jsonPrimitive.content }
    suspend fun relations(keys: Set<String>, type: DataType): List<DTagRelation> = RustContentApi.query("tagRelations(type: ${type.name}, keys: ${gqlIds(keys)}) { tagId key }").getValue("tagRelations").jsonArray.map { DTagRelation(tagId=it.jsonObject.string("tagId"),key=it.jsonObject.string("key"),type=type.value) }
    suspend fun edit(key: String, type: Int, add: Collection<String>, remove: Collection<String>) {
        val kind = DataType.entries.first { it.value == type }
        RustContentApi.mutate("updateTagRelations(type: ${kind.name}, item: { key: ${gql(key)}, title: \"\", size: 0 }, addTagIds: ${gqlIds(add)}, removeTagIds: ${gqlIds(remove)})")
    }
    suspend fun remove(keys: Set<String>, tagIds: Set<String>) {
        tagIds.forEach { id ->
            val tag = get(id) ?: return@forEach
            val type = DataType.entries.first { it.value == tag.type }
            RustContentApi.mutate("removeFromTags(type: ${type.name}, tagIds: ${gqlIds(listOf(id))}, query: ${gql(selectionQuery(keys))})")
        }
    }
}
