package com.ismartcoding.plain.features

import com.ismartcoding.plain.api.RustContentTags
import com.ismartcoding.plain.data.TagRelationStub
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.db.DTagCount
import com.ismartcoding.plain.db.DTagRelation
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.lib.withIO

object TagHelper {
    suspend fun count(type: DataType): List<DTagCount> = getAll(type).map { DTagCount(it.id, it.count) }
    suspend fun getAll(type: DataType): List<DTag> = RustContentTags.all(type)
    suspend fun get(id: String): DTag? = RustContentTags.get(id)
    suspend fun addOrUpdate(id: String, updateItem: DTag.() -> Unit): String = withIO {
        val create = id.isEmpty()
        val item = if (create) DTag() else get(id) ?: error("Tag $id not found")
        updateItem(item)
        RustContentTags.save(item, create)
    }
    suspend fun delete(id: String) = RustContentTags.delete(id)
    suspend fun getTagRelationsByKeys(keys: Set<String>, type: DataType): List<DTagRelation> = RustContentTags.relations(keys, type)
    suspend fun getTagRelationsByKeysMap(keys: Set<String>, type: DataType): Map<String, List<DTagRelation>> = getTagRelationsByKeys(keys, type).groupBy { it.key }
    suspend fun getTagRelationsByKey(key: String, type: DataType): List<DTagRelation> = getTagRelationsByKeys(setOf(key), type)
    suspend fun getKeysByTagId(tagId: String): List<String> = RustContentTags.keys(tagId)
    suspend fun getKeysByTagIdsAsync(tagIds: Set<String>): List<String> = RustContentTags.intersection(tagIds)
    suspend fun addTagRelations(items: List<DTagRelation>) = RustContentTags.add(items)
    suspend fun editTagRelations(type: DataType, item: TagRelationStub, add: Collection<String>, remove: Collection<String>) = RustContentTags.edit(item.key, type.value, add, remove, item.title, item.size)
    suspend fun deleteTagRelationsByTagId(tagId: String) = RustContentTags.clearTag(tagId)
    suspend fun deleteByTypeAsync(type: DataType) = RustContentTags.clearType(type)
    suspend fun deleteTagRelationByKeys(keys: Set<String>, type: DataType) = RustContentTags.removeKeys(keys, type)
    suspend fun deleteTagRelationByKeysTagId(keys: Set<String>, tagId: String) = RustContentTags.remove(keys, setOf(tagId))
    suspend fun deleteTagRelationByKeysTagIds(keys: Set<String>, tagIds: Set<String>) = RustContentTags.remove(keys, tagIds)
}
