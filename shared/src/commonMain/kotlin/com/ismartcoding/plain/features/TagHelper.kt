package com.ismartcoding.plain.features

import com.ismartcoding.plain.helpers.withIO
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

    suspend fun count(type: DataType): List<DTagCount> = withIO {
        tagRelationDao.getAll(type.value)
    }

    suspend fun getAll(type: DataType): List<DTag> = withIO {
        tagDao.getAll(type.value)
    }

    suspend fun get(id: String): DTag? = withIO {
        tagDao.getById(id)
    }

    suspend fun addOrUpdate(id: String, updateItem: DTag.() -> Unit): String = withIO {
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

        item.id
    }

    suspend fun delete(id: String) = withIO {
        tagDao.delete(id)
    }

    suspend fun getTagRelationsByKeys(
        keys: Set<String>,
        type: DataType,
    ): List<DTagRelation> = withIO {
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
        tagRelationDao.getAllByKey(key, type.value)
    }

    suspend fun getKeysByTagId(tagId: String): List<String> = withIO {
        tagRelationDao.getKeysByTagId(tagId)
    }

    suspend fun getKeysByTagIdsAsync(tagIds: Set<String>): List<String> = withIO {
        val items = tagRelationDao.getAllByTagIds(tagIds)
        items.groupBy { it.key }.filter { it.value.size == tagIds.size }.map { it.key }
    }

    suspend fun addTagRelations(items: List<DTagRelation>) = withIO {
        tagRelationDao.insert(*items.toTypedArray())
    }

    suspend fun deleteTagRelationsByTagId(tagId: String) = withIO {
        tagRelationDao.deleteByTagId(tagId)
    }

    suspend fun deleteByTypeAsync(type: DataType) = withIO {
        tagRelationDao.deleteByType(type.value)
    }

    suspend fun deleteTagRelationByKeys(keys: Set<String>, type: DataType) = withIO {
        keys.chunked(50).forEach { chunk ->
            tagRelationDao.deleteByKeys(chunk.toSet(), type.value)
        }
    }

    suspend fun deleteTagRelationByKeysTagId(keys: Set<String>, tagId: String) = withIO {
        keys.chunked(50).forEach { chunk ->
            tagRelationDao.deleteByKeysTagId(chunk.toSet(), tagId)
        }
    }

    suspend fun deleteTagRelationByKeysTagIds(keys: Set<String>, tagIds: Set<String>) = withIO {
        keys.chunked(50).forEach { chunk ->
            tagRelationDao.deleteByKeysTagIds(chunk.toSet(), tagIds)
        }
    }
}
