package com.ismartcoding.plain.ui.models

import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.data.TagRelationStub
import com.ismartcoding.plain.features.TagHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal fun TagsViewModel.removeFromTags(ids: Set<String>, tagIds: Set<String>) {
    viewModelScope.launch(Dispatchers.IO) {
        for (tagId in tagIds) {
            TagHelper.deleteTagRelationByKeysTagId(ids, tagId)
        }
        for (id in ids) {
            tagsMapFlow.value.toMutableMap().let { map ->
                map[id] = map[id]?.filter { !tagIds.contains(it.tagId) } ?: emptyList()
                updateTagsMap(map)
            }
        }
        loadAsync()
    }
}

internal fun TagsViewModel.addToTags(items: List<IData>, tagIds: Set<String>) {
    viewModelScope.launch(Dispatchers.IO) {
        for (tagId in tagIds) {
            val existingKeys = TagHelper.getKeysByTagId(tagId)
            val newItems = items.filter { !existingKeys.contains(it.id) }
            if (newItems.isNotEmpty()) {
                val relations = newItems.map { item ->
                    TagRelationStub.create(item).toTagRelation(tagId, dataType.value)
                }
                TagHelper.addTagRelations(relations)
                val mutableMap = tagsMapFlow.value.toMutableMap()
                for (item in newItems) {
                    val id = item.id
                    mutableMap[id] = mutableMap[id]?.toMutableList()?.apply {
                        addAll(relations.filter { it.key == id })
                    } ?: relations.filter { it.key == id }
                }
                updateTagsMap(mutableMap)
            }
        }
        loadAsync()
    }
}

internal suspend fun TagsViewModel.toggleTagAsync(data: IData, tagId: String) {
    val tagIds = tagsMapFlow.value[data.id]?.map { it.tagId } ?: emptyList()
    try {
        if (tagIds.contains(tagId)) {
            TagHelper.deleteTagRelationByKeysTagId(setOf(data.id), tagId)
            val mutableMap = tagsMapFlow.value.toMutableMap()
            mutableMap[data.id] = mutableMap[data.id]?.filter { it.tagId != tagId } ?: emptyList()
            updateTagsMap(mutableMap)
        } else {
            val relation = TagRelationStub.create(data).toTagRelation(tagId, dataType.value)
            TagHelper.addTagRelations(listOf(relation))
            val mutableMap = tagsMapFlow.value.toMutableMap()
            mutableMap[data.id] = mutableMap[data.id]?.toMutableList()?.apply {
                add(relation)
            } ?: listOf(relation)
            updateTagsMap(mutableMap)
        }
        loadAsync()
    } catch (ex: Exception) {
        LogCat.e(ex.toString())
    }
}
