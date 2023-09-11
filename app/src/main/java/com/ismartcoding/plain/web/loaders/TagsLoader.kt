package com.ismartcoding.plain.web.loaders

import com.ismartcoding.plain.data.enums.TagType
import com.ismartcoding.plain.features.tag.TagHelper
import com.ismartcoding.plain.web.models.Tag
import com.ismartcoding.plain.web.models.toModel
import nidomiro.kdataloader.BatchLoader
import nidomiro.kdataloader.ExecutionResult

object TagsLoader {
    fun load(ids: List<String>, tagType: TagType): List<ExecutionResult<List<Tag>>> {
        val tagRelations = TagHelper.getTagRelationsByKeys(ids.toSet(), tagType).groupBy { it.key }
        val tags = TagHelper.getAll(tagType).associateBy { it.id }
        return ids.map { id ->
            val tagIds = tagRelations[id]?.map { it.tagId } ?: setOf()
            ExecutionResult.Success(if (tagIds.isEmpty()) {
                listOf()
            } else {
                val list = mutableListOf<Tag>()
                tagIds.forEach { tagId ->
                    tags[tagId]?.toModel()?.let {
                        list.add(it)
                    }
                }
                list
            })
        }
    }

    fun load(id: String, tagType: TagType): List<Tag> {
        val tagRelations = TagHelper.getTagRelationsByKey(id, tagType)
        val tags = TagHelper.getAll(tagType).associateBy { it.id }
        val tagIds = tagRelations.map { it.tagId }
        return if (tagIds.isEmpty()) {
            listOf()
        } else {
            val list = mutableListOf<Tag>()
            tagIds.forEach { tagId ->
                tags[tagId]?.toModel()?.let {
                    list.add(it)
                }
            }
            list
        }
    }
}