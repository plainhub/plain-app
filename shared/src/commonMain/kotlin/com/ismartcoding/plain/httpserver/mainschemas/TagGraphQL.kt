package com.ismartcoding.plain.httpserver.mainschemas

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.data.TagRelationStub
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.NoteHelper
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.features.feed.FeedEntryHelper
import com.ismartcoding.plain.platform.getMediaIds
import com.ismartcoding.plain.platform.getMediaTagRelationStubs
import com.ismartcoding.plain.httpserver.models.ID
import com.ismartcoding.plain.httpserver.models.Tag
import com.ismartcoding.plain.httpserver.models.TagRelation
import com.ismartcoding.plain.httpserver.models.toModel

@GraphQLQuery
suspend fun tags(type: DataType): List<Tag> {
    val tagCountMap = TagHelper.count(type).associate { it.id to it.count }
    return TagHelper.getAll(type).map {
        it.count = tagCountMap[it.id] ?: 0
        it.toModel()
    }
}

@GraphQLQuery
suspend fun tagRelations(type: DataType, keys: List<String>): List<TagRelation> {
    return TagHelper.getTagRelationsByKeys(keys.toSet(), type).map { it.toModel() }
}

@GraphQLMutation
suspend fun createTag(type: DataType, name: String): Tag? {
    val id =
        TagHelper.addOrUpdate("") {
            this.name = name
            this.type = type.value
        }
    return TagHelper.get(id)?.toModel()
}

@GraphQLMutation
suspend fun updateTag(id: ID, name: String): Tag? {
    TagHelper.addOrUpdate(id.value) {
        this.name = name
    }
    return TagHelper.get(id.value)?.toModel()
}

@GraphQLMutation
suspend fun deleteTag(id: ID): Boolean {
    TagHelper.deleteTagRelationsByTagId(id.value)
    TagHelper.delete(id.value)
    return true
}

@GraphQLMutation
suspend fun addToTags(type: DataType, tagIds: List<ID>, query: String): Boolean {
    val items: List<TagRelationStub> = when (type) {
        DataType.AUDIO, DataType.VIDEO, DataType.IMAGE, DataType.DOC, DataType.CALL, DataType.CONTACT ->
            getMediaTagRelationStubs(type, query)

        DataType.SMS -> getMediaIds(type, query).map { TagRelationStub(it) }

        DataType.NOTE -> NoteHelper.getIdsAsync(query).map { TagRelationStub(it) }

        DataType.FEED_ENTRY -> FeedEntryHelper.getIdsAsync(query).map { TagRelationStub(it) }

        else -> emptyList()
    }

    tagIds.forEach { tagId ->
        val existingKeys = withIO { TagHelper.getKeysByTagId(tagId.value) }
        val newItems = items.filter { !existingKeys.contains(it.key) }
        if (newItems.isNotEmpty()) {
            TagHelper.addTagRelations(
                newItems.map {
                    it.toTagRelation(tagId.value, type)
                },
            )
        }
    }
    return true
}

@GraphQLMutation
suspend fun updateTagRelations(type: DataType, item: TagRelationStub, addTagIds: List<ID>, removeTagIds: List<ID>): Boolean {
    addTagIds.forEach { tagId ->
        TagHelper.addTagRelations(
            arrayOf(item).map {
                it.toTagRelation(tagId.value, type)
            },
        )
    }
    if (removeTagIds.isNotEmpty()) {
        TagHelper.deleteTagRelationByKeysTagIds(setOf(item.key), removeTagIds.map { it.value }.toSet())
    }
    return true
}

@GraphQLMutation
suspend fun removeFromTags(type: DataType, tagIds: List<ID>, query: String): Boolean {
    val ids = when (type) {
        DataType.AUDIO, DataType.VIDEO, DataType.IMAGE, DataType.DOC, DataType.CALL,
        DataType.CONTACT, DataType.SMS,
        -> getMediaIds(type, query)

        DataType.NOTE -> NoteHelper.getIdsAsync(query)
        DataType.FEED_ENTRY -> FeedEntryHelper.getIdsAsync(query)
        else -> emptySet()
    }

    TagHelper.deleteTagRelationByKeysTagIds(ids, tagIds.map { it.value }.toSet())
    return true
}

fun SchemaBuilder.addTagSchema() {
}
