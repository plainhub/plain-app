package com.ismartcoding.plain.web.loaders

import com.ismartcoding.plain.data.enums.DataType
import com.ismartcoding.plain.features.tag.TagHelper
import com.ismartcoding.plain.web.models.Tag
import com.ismartcoding.plain.web.models.toModel
import nidomiro.kdataloader.ExecutionResult

object TagsLoader {
    fun load(
        ids: List<String>,
        type: DataType,
    ): List<ExecutionResult<List<Tag>>> {
        val tagRelations = TagHelper.getTagRelationsByKeys(ids.toSet(), type).groupBy { it.key }
        val tags = TagHelper.getAll(type).associateBy { it.id }
        return ids.map { id ->
            val tagIds = tagRelations[id]?.map { it.tagId } ?: setOf()
            ExecutionResult.Success(
                if (tagIds.isEmpty()) {
                    listOf()
                } else {
                    val list = mutableListOf<Tag>()
                    tagIds.forEach { tagId ->
                        tags[tagId]?.toModel()?.let {
                            list.add(it)
                        }
                    }
                    list
                },
            )
        }
    }

    fun load(
        id: String,
        type: DataType,
    ): List<Tag> {
        val tagRelations = TagHelper.getTagRelationsByKey(id, type)
        val tags = TagHelper.getAll(type).associateBy { it.id }
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
