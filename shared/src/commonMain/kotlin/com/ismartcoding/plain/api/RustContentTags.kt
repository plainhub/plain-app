package com.ismartcoding.plain.api

import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.db.DTagRelation
import com.ismartcoding.plain.enums.DataType
import kotlinx.serialization.json.*

internal object RustContentTags {
    private const val fields = "id name type count createdAt updatedAt"
    private fun kind(type: Int): DataType = DataType.entries.first { it.value == type }
    private fun JsonElement.record(): DTag = jsonObject.let { DTag(id=it.string("id"), name=it.string("name"), type=it.getValue("type").jsonPrimitive.int, count=it.getValue("count").jsonPrimitive.int, createdAt=it.instant("createdAt"), updatedAt=it.instant("updatedAt")) }
    suspend fun all(type: DataType): List<DTag> = RustContentApi.query("tagHostAll(type: ${type.name}) { $fields }").getValue("tagHostAll").jsonArray.map { it.record() }
    suspend fun get(id: String): DTag? = RustContentApi.query("tagHostGet(id: ${gql(id)}) { $fields }")["tagHostGet"]?.takeUnless { it is JsonNull }?.record()
    suspend fun save(item: DTag, create: Boolean): String {
        val field = if (create) "createTag" else "updateTag"
        val args = if (create) "type: ${kind(item.type).name}, name: ${gql(item.name)}" else "id: ${gql(item.id)}, name: ${gql(item.name)}"
        return RustContentApi.mutate("$field($args) { id }").getValue(field).jsonObject.string("id")
    }
    suspend fun delete(id: String) { RustContentApi.mutate("deleteTag(id: ${gql(id)})") }
    suspend fun keys(id: String): List<String> = RustContentApi.query("tagKeys(id: ${gql(id)})").getValue("tagKeys").jsonArray.map { it.jsonPrimitive.content }
    suspend fun intersection(ids: Collection<String>): List<String> = RustContentApi.query("tagHostIntersection(tagIds: ${gqlIds(ids)})").getValue("tagHostIntersection").jsonArray.map { it.jsonPrimitive.content }
    suspend fun relations(keys: Set<String>, type: DataType): List<DTagRelation> = RustContentApi.query("tagHostRelations(type: ${type.name}, keys: ${gqlIds(keys)}) { tagId key type title sizeBytes createdAt }").getValue("tagHostRelations").jsonArray.map { value -> value.jsonObject.let { DTagRelation(tagId=it.string("tagId"), key=it.string("key"), type=it.getValue("type").jsonPrimitive.int, title=it.string("title"), size=it.getValue("sizeBytes").jsonPrimitive.long, createdAt=it.instant("createdAt")) } }
    suspend fun add(items: List<DTagRelation>) {
        val input = items.joinToString(",", "[", "]") { "{ tagId: ${gql(it.tagId)}, key: ${gql(it.key)}, type: ${kind(it.type).name}, title: ${gql(it.title)}, sizeBytes: ${it.size} }" }
        RustContentApi.mutate("tagHostAdd(items: $input)")
    }
    suspend fun edit(key: String, type: Int, add: Collection<String>, remove: Collection<String>, title: String = "", size: Long = 0) {
        RustContentApi.mutate("tagHostEdit(type: ${kind(type).name}, item: { key: ${gql(key)}, title: ${gql(title)}, sizeBytes: $size }, addTagIds: ${gqlIds(add)}, removeTagIds: ${gqlIds(remove)})")
    }
    suspend fun remove(keys: Set<String>, tagIds: Set<String>) { RustContentApi.mutate("tagHostRemove(keys: ${gqlIds(keys)}, tagIds: ${gqlIds(tagIds)})") }
    suspend fun removeKeys(keys: Set<String>, type: DataType) { RustContentApi.mutate("tagHostRemoveKeys(type: ${type.name}, keys: ${gqlIds(keys)})") }
    suspend fun clearType(type: DataType) { RustContentApi.mutate("tagHostClearType(type: ${type.name})") }
    suspend fun clearTag(id: String) { RustContentApi.mutate("tagHostClearTag(id: ${gql(id)})") }
}
