package com.ismartcoding.plain.features

import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.db.DTagCount
import com.ismartcoding.plain.db.DTagRelation
import com.ismartcoding.plain.db.TagDao
import com.ismartcoding.plain.db.TagRelationDao
import com.ismartcoding.plain.helpers.TimeHelper

object TagHelper {
    private val tagDao: TagDao by lazy {
        AppDatabase.instance.tagDao()
    }

    private val tagRelationDao: TagRelationDao by lazy {
        AppDatabase.instance.tagRelationDao()
    }

    fun count(type: DataType): List<DTagCount> {
        return tagRelationDao.getAll(type.value)
    }

    fun getAll(type: DataType): List<DTag> {
        return tagDao.getAll(type.value)
    }

    fun get(id: String): DTag? {
        return tagDao.getById(id)
    }

    fun addOrUpdate(id: String, updateItem: DTag.() -> Unit): String {
        var item = if (id.isNotEmpty()) tagDao.getById(id) else null
        var isInsert = false
        if (item == null) {
            item = DTag()
            isInsert = true
        }

        item.updatedAt = TimeHelper.now()

        updateItem(item)

        if (isInsert) {
            tagDao.insert(item)
        } else {
            tagDao.update(item)
        }

        return item.id
    }

    fun delete(id: String) {
        return tagDao.delete(id)
    }

    fun getTagRelationsByKeys(
        keys: Set<String>,
        type: DataType,
    ): List<DTagRelation> {
        val items = mutableListOf<DTagRelation>()
        keys.chunked(50).forEach { chunk ->
            items.addAll(tagRelationDao.getAllByKeys(chunk.toSet(), type.value))
        }
        return items
    }

    fun getTagRelationsByKeysMap(
        keys: Set<String>,
        type: DataType,
    ): Map<String, List<DTagRelation>> {
        return getTagRelationsByKeys(keys, type).groupBy { it.key }
    }

    fun getTagRelationsByKey(
        key: String,
        type: DataType,
    ): List<DTagRelation> {
        return tagRelationDao.getAllByKey(key, type.value)
    }

    fun getKeysByTagId(tagId: String): List<String> {
        return tagRelationDao.getKeysByTagId(tagId)
    }

    suspend fun getKeysByTagIdsAsync(tagIds: Set<String>): List<String> {
        val items = tagRelationDao.getAllByTagIds(tagIds)
        return items.groupBy { it.key }.filter { it.value.size == tagIds.size }.map { it.key }
    }

    fun addTagRelations(items: List<DTagRelation>) {
        tagRelationDao.insert(*items.toTypedArray())
    }

    fun deleteTagRelationsByTagId(tagId: String) {
        tagRelationDao.deleteByTagId(tagId)
    }

    fun deleteByTypeAsync(type: DataType) {
        tagRelationDao.deleteByType(type.value)
    }

    fun deleteTagRelationByKeys(keys: Set<String>, type: DataType) {
        keys.chunked(50).forEach { chunk ->
            tagRelationDao.deleteByKeys(chunk.toSet(), type.value)
        }
    }

    fun deleteTagRelationByKeysTagId(keys: Set<String>, tagId: String) {
        keys.chunked(50).forEach { chunk ->
            tagRelationDao.deleteByKeysTagId(chunk.toSet(), tagId)
        }
    }

    fun deleteTagRelationByKeysTagIds(keys: Set<String>, tagIds: Set<String>) {
        keys.chunked(50).forEach { chunk ->
            tagRelationDao.deleteByKeysTagIds(chunk.toSet(), tagIds)
        }
    }
}
