package com.ismartcoding.plain.web.schemas

import com.ismartcoding.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.data.TagRelationStub
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.NoteHelper
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.features.feed.FeedEntryHelper
import com.ismartcoding.plain.features.media.AudioMediaStoreHelper
import com.ismartcoding.plain.features.media.CallMediaStoreHelper
import com.ismartcoding.plain.features.media.ContactMediaStoreHelper
import com.ismartcoding.plain.features.media.ImageMediaStoreHelper
import com.ismartcoding.plain.features.media.VideoMediaStoreHelper
import com.ismartcoding.plain.features.sms.SmsHelper
import com.ismartcoding.plain.web.models.ID
import com.ismartcoding.plain.web.models.toModel

fun SchemaBuilder.addTagSchema() {
    query("tags") {
        resolver { type: DataType ->
            val tagCountMap = TagHelper.count(type).associate { it.id to it.count }
            TagHelper.getAll(type).map {
                it.count = tagCountMap[it.id] ?: 0
                it.toModel()
            }
        }
    }
    query("tagRelations") {
        resolver { type: DataType, keys: List<String> ->
            TagHelper.getTagRelationsByKeys(keys.toSet(), type).map { it.toModel() }
        }
    }
    mutation("createTag") {
        resolver { type: DataType, name: String ->
            val id =
                TagHelper.addOrUpdate("") {
                    this.name = name
                    this.type = type.value
                }
            TagHelper.get(id)?.toModel()
        }
    }
    mutation("updateTag") {
        resolver { id: ID, name: String ->
            TagHelper.addOrUpdate(id.value) {
                this.name = name
            }
            TagHelper.get(id.value)?.toModel()
        }
    }
    mutation("deleteTag") {
        resolver { id: ID ->
            TagHelper.deleteTagRelationsByTagId(id.value)
            TagHelper.delete(id.value)
            true
        }
    }
    mutation("addToTags") {
        resolver { type: DataType, tagIds: List<ID>, query: String ->
            var items = listOf<TagRelationStub>()
            val context = MainApp.instance
            when (type) {
                DataType.AUDIO -> {
                    items = AudioMediaStoreHelper.getTagRelationStubsAsync(context, query)
                }

                DataType.VIDEO -> {
                    items = VideoMediaStoreHelper.getTagRelationStubsAsync(context, query)
                }

                DataType.IMAGE -> {
                    items = ImageMediaStoreHelper.getTagRelationStubsAsync(context, query)
                }

                DataType.SMS -> {
                    items = SmsHelper.getIdsAsync(context, query).map { TagRelationStub(it) }
                }

                DataType.CONTACT -> {
                    items = ContactMediaStoreHelper.getIdsAsync(context, query).map { TagRelationStub(it) }
                }

                DataType.NOTE -> {
                    items = NoteHelper.getIdsAsync(query).map { TagRelationStub(it) }
                }

                DataType.FEED_ENTRY -> {
                    items = FeedEntryHelper.getIdsAsync(query).map { TagRelationStub(it) }
                }

                DataType.CALL -> {
                    items = CallMediaStoreHelper.getIdsAsync(context, query).map { TagRelationStub(it) }
                }

                else -> {}
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
        resolver { type: DataType, item: TagRelationStub, addTagIds: List<ID>, removeTagIds: List<ID> ->
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
        resolver { type: DataType, tagIds: List<ID>, query: String ->
            val context = MainApp.instance
            var ids = setOf<String>()
            when (type) {
                DataType.AUDIO -> {
                    ids = AudioMediaStoreHelper.getIdsAsync(context, query)
                }

                DataType.VIDEO -> {
                    ids = VideoMediaStoreHelper.getIdsAsync(context, query)
                }

                DataType.IMAGE -> {
                    ids = ImageMediaStoreHelper.getIdsAsync(context, query)
                }

                DataType.SMS -> {
                    ids = SmsHelper.getIdsAsync(context, query)
                }

                DataType.CONTACT -> {
                    ids = ContactMediaStoreHelper.getIdsAsync(context, query)
                }

                DataType.NOTE -> {
                    ids = NoteHelper.getIdsAsync(query)
                }

                DataType.FEED_ENTRY -> {
                    ids = FeedEntryHelper.getIdsAsync(query)
                }

                DataType.CALL -> {
                    ids = CallMediaStoreHelper.getIdsAsync(context, query)
                }

                else -> {}
            }

            TagHelper.deleteTagRelationByKeysTagIds(ids, tagIds.map { it.value }.toSet())
            true
        }
    }
}
