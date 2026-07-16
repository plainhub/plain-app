package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.data.TagRelationStub
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.NoteHelper
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.features.feed.FeedEntryHelper
import com.ismartcoding.plain.platform.getMediaIds
import com.ismartcoding.plain.platform.getMediaTagRelationStubs
import com.ismartcoding.plain.web.models.ID
import com.ismartcoding.plain.web.models.toModel

fun SchemaBuilder.addTagSchema() {
    query("tags") {
        resolver("type") { type: DataType ->
            val tagCountMap = TagHelper.count(type).associate { it.id to it.count }
            TagHelper.getAll(type).map {
                it.count = tagCountMap[it.id] ?: 0
                it.toModel()
            }
        }
    }
    query("tagRelations") {
        resolver("type", "keys") { type: DataType, keys: List<String> ->
            TagHelper.getTagRelationsByKeys(keys.toSet(), type).map { it.toModel() }
        }
    }
    mutation("createTag") {
        resolver("type", "name") { type: DataType, name: String ->
            val id =
                TagHelper.addOrUpdate("") {
                    this.name = name
                    this.type = type.value
                }
            TagHelper.get(id)?.toModel()
        }
    }
    mutation("updateTag") {
        resolver("id", "name") { id: ID, name: String ->
            TagHelper.addOrUpdate(id.value) {
                this.name = name
            }
            TagHelper.get(id.value)?.toModel()
        }
    }
    mutation("deleteTag") {
        resolver("id") { id: ID ->
            TagHelper.deleteTagRelationsByTagId(id.value)
            TagHelper.delete(id.value)
            true
        }
    }
    mutation("addToTags") {
        resolver("type", "tagIds", "query") { type: DataType, tagIds: List<ID>, query: String ->
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
            true
        }
    }
    mutation("updateTagRelations") {
        resolver("type", "item", "addTagIds", "removeTagIds") { type: DataType, item: TagRelationStub, addTagIds: List<ID>, removeTagIds: List<ID> ->
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
            true
        }
    }
    mutation("removeFromTags") {
        resolver("type", "tagIds", "query") { type: DataType, tagIds: List<ID>, query: String ->
            val ids = when (type) {
                DataType.AUDIO, DataType.VIDEO, DataType.IMAGE, DataType.DOC, DataType.CALL,
                DataType.CONTACT, DataType.SMS,
                -> getMediaIds(type, query)

                DataType.NOTE -> NoteHelper.getIdsAsync(query)
                DataType.FEED_ENTRY -> FeedEntryHelper.getIdsAsync(query)
                else -> emptySet()
            }

            TagHelper.deleteTagRelationByKeysTagIds(ids, tagIds.map { it.value }.toSet())
            true
        }
    }
}
