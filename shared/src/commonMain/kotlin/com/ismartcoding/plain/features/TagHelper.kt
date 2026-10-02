package com.ismartcoding.plain.features

import com.ismartcoding.plain.api.RustContentTags
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.db.DTagCount
import com.ismartcoding.plain.db.DTagRelation
import com.ismartcoding.plain.db.TagDao
import com.ismartcoding.plain.db.TagRelationDao
import com.ismartcoding.plain.lib.TimeHelper

object TagHelper {
    private val tagDao: TagDao by lazy {
        AppDatabase.instance.tagDao()
    }

    private val tagRelationDao: TagRelationDao by lazy {
        AppDatabase.instance.tagRelationDao()
    }

    suspend fun count(type: DataType): List<DTagCount> = withIO {
        if (RustContentTags.owns(type.value)) return@withIO RustContentTags.all(type).map { DTagCount(it.id, it.count) }
        tagRelationDao.getAll(type.value)
    }

    suspend fun getAll(type: DataType): List<DTag> = withIO {
        if (RustContentTags.owns(type.value)) return@withIO RustContentTags.all(type)
        tagDao.getAll(type.value)
    }

    suspend fun get(id: String): DTag? = withIO {
        RustContentTags.get(id)?.let { return@withIO it }
        tagDao.getById(id)?.takeUnless { RustContentTags.owns(it.type) }
    }

    suspend fun addOrUpdate(id: String, updateItem: DTag.() -> Unit): String = withIO {
        var item = if (id.isNotEmpty()) get(id) else null
        var isInsert = false
        if (item == null) {
            item = DTag()
            isInsert = true
        }

        item.updatedAt = TimeHelper.now()

        updateItem(item)

        if (RustContentTags.owns(item.type)) return@withIO RustContentTags.save(item, isInsert)

        if (isInsert) {
            tagDao.insert(item)
        } else {
            tagDao.update(item)
        }

        item.id
    }

    suspend fun delete(id: String) = withIO {
        if (RustContentTags.get(id) != null) { RustContentTags.delete(id); return@withIO }
        tagDao.delete(id)
    }

    suspend fun getTagRelationsByKeys(
        keys: Set<String>,
        type: DataType,
    ): List<DTagRelation> = withIO {
        if (RustContentTags.owns(type.value)) return@withIO RustContentTags.relations(keys, type)
        val items = mutableListOf<DTagRelation>()
        keys.chunked(50).forEach { chunk ->
            items.addAll(tagRelationDao.getAllByKeys(chunk.toSet(), type.value))
        }
        items
    }

    suspend fun getTagRelationsByKeysMap(
        keys: Set<String>,
        type: DataType,
    ): Map<String, List<DTagRelation>> = withIO {
        getTagRelationsByKeys(keys, type).groupBy { it.key }
    }

    suspend fun getTagRelationsByKey(
        key: String,
        type: DataType,
    ): List<DTagRelation> = withIO {
        if (RustContentTags.owns(type.value)) return@withIO RustContentTags.relations(setOf(key), type)
        tagRelationDao.getAllByKey(key, type.value)
    }

    suspend fun getKeysByTagId(tagId: String): List<String> = withIO {
        if (RustContentTags.get(tagId) != null) return@withIO RustContentTags.keys(tagId)
        tagRelationDao.getKeysByTagId(tagId)
    }

    suspend fun getKeysByTagIdsAsync(tagIds: Set<String>): List<String> = withIO {
        if (tagIds.isNotEmpty() && RustContentTags.get(tagIds.first()) != null) return@withIO tagIds.map { getKeysByTagId(it).toSet() }.reduce { a, b -> a.intersect(b) }.toList()
        val items = tagRelationDao.getAllByTagIds(tagIds)
        items.groupBy { it.key }.filter { it.value.size == tagIds.size }.map { it.key }
    }

    suspend fun addTagRelations(items: List<DTagRelation>) = withIO {
        val remote = items.filter { RustContentTags.owns(it.type) }
        remote.groupBy { it.key to it.type }.forEach { (key, values) -> RustContentTags.edit(key.first, key.second, values.map { it.tagId }, emptyList()) }
        val local = items.filterNot { RustContentTags.owns(it.type) }
        if (local.isNotEmpty()) tagRelationDao.insert(*local.toTypedArray())
    }

    suspend fun deleteTagRelationsByTagId(tagId: String) = withIO {
        if (RustContentTags.get(tagId) != null) { val keys = RustContentTags.keys(tagId).toSet(); if (keys.isNotEmpty()) RustContentTags.remove(keys, setOf(tagId)); return@withIO }
        tagRelationDao.deleteByTagId(tagId)
    }

    suspend fun deleteByTypeAsync(type: DataType) = withIO {
        if (RustContentTags.owns(type.value)) { RustContentTags.all(type).forEach { tag -> val keys = RustContentTags.keys(tag.id).toSet(); if (keys.isNotEmpty()) RustContentTags.remove(keys, setOf(tag.id)) }; return@withIO }
        tagRelationDao.deleteByType(type.value)
    }

    suspend fun deleteTagRelationByKeys(keys: Set<String>, type: DataType) = withIO {
        if (RustContentTags.owns(type.value)) { val relations = RustContentTags.relations(keys, type); if (keys.isNotEmpty()) RustContentTags.remove(keys, relations.map { it.tagId }.toSet()); return@withIO }
        keys.chunked(50).forEach { chunk ->
            tagRelationDao.deleteByKeys(chunk.toSet(), type.value)
        }
    }

    suspend fun deleteTagRelationByKeysTagId(keys: Set<String>, tagId: String) = withIO {
        if (RustContentTags.get(tagId) != null) { if (keys.isNotEmpty()) RustContentTags.remove(keys, setOf(tagId)); return@withIO }
        keys.chunked(50).forEach { chunk ->
            tagRelationDao.deleteByKeysTagId(chunk.toSet(), tagId)
        }
    }

    suspend fun deleteTagRelationByKeysTagIds(keys: Set<String>, tagIds: Set<String>) = withIO {
        val remote = tagIds.filter { RustContentTags.get(it) != null }.toSet()
        if (keys.isNotEmpty()) RustContentTags.remove(keys, remote)
        val local = tagIds - remote
        if (local.isEmpty()) return@withIO
        keys.chunked(50).forEach { chunk ->
            tagRelationDao.deleteByKeysTagIds(chunk.toSet(), local)
        }
    }
}
