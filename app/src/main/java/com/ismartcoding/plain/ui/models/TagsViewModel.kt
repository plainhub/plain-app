package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.db.DTagRelation
import com.ismartcoding.plain.features.TagHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi::class)
class TagsViewModel() : ViewModel() {
    private val _itemsFlow = MutableStateFlow(mutableStateListOf<DTag>())
    val itemsFlow: StateFlow<List<DTag>> get() = _itemsFlow
    private val _tagsMapFlow = MutableStateFlow(mutableMapOf<String, List<DTagRelation>>())
    val tagsMapFlow: StateFlow<Map<String, List<DTagRelation>>> get() = _tagsMapFlow
    var showLoading = mutableStateOf(true)
    var tagNameDialogVisible = mutableStateOf(false)
    var editItem = mutableStateOf<DTag?>(null)
    var editTagName = mutableStateOf("")
    var dataType = mutableStateOf(DataType.DEFAULT)

    fun loadAsync(keys: Set<String> = emptySet()) {
        if (keys.isNotEmpty()) {
            _tagsMapFlow.value = TagHelper.getTagRelationsByKeysMap(keys, dataType.value).toMutableMap()
        }
        val tagCountMap = TagHelper.count(dataType.value).associate { it.id to it.count }
        _itemsFlow.value = TagHelper.getAll(dataType.value).map { tag ->
            tag.count = tagCountMap[tag.id] ?: 0
            tag
        }.toMutableStateList()
        showLoading.value = false
    }

    fun loadMoreAsync(keys: Set<String>) {
        if (keys.isNotEmpty()) {
            _tagsMapFlow.value += TagHelper.getTagRelationsByKeysMap(keys, dataType.value)
        }
    }

    fun addTag(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = TagHelper.addOrUpdate("") {
                this.name = name
                type = dataType.value.value
            }
            _itemsFlow.update {
                val mutableList = it.toMutableStateList()
                mutableList.add(DTag(id).apply {
                    this.name = name
                    type = dataType.value.value
                })
                mutableList
            }
        }
        tagNameDialogVisible.value = false
    }

    fun editTag(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = TagHelper.addOrUpdate(editItem.value!!.id) {
                this.name = name
            }
            _itemsFlow.update {
                it.toMutableStateList().apply {
                    find { i -> i.id == id }?.name = name
                }
            }
        }
        tagNameDialogVisible.value = false
    }

    fun deleteTag(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            TagHelper.deleteTagRelationsByTagId(id)
            TagHelper.delete(id)
            _itemsFlow.update {
                it.toMutableStateList().apply {
                    removeIf { i -> i.id == id }
                }
            }
            for (key in _tagsMapFlow.value.keys) {
                _tagsMapFlow.value[key] = _tagsMapFlow.value[key]?.filter { it.tagId != id } ?: emptyList()
            }
        }
    }

    fun removeFromTags(ids: Set<String>, tagIds: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            for (tagId in tagIds) {
                TagHelper.deleteTagRelationByKeysTagId(ids, tagId)
            }
            for (id in ids) {
                _tagsMapFlow.value[id] = _tagsMapFlow.value[id]?.filter { !tagIds.contains(it.tagId) } ?: emptyList()
            }
            loadAsync()
        }
    }

    fun addToTags(ids: Set<String>, tagIds: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            for (tagId in tagIds) {
                val existingKeys = TagHelper.getKeysByTagId(tagId)
                val newIds = ids.filter { !existingKeys.contains(it) }
                if (newIds.isNotEmpty()) {
                    val relations = newIds.map { id ->
                        DTagRelation(tagId, id, dataType.value.value)
                    }
                    TagHelper.addTagRelations(relations)
                    for (id in newIds) {
                        _tagsMapFlow.value[id] = _tagsMapFlow.value[id]?.toMutableList()?.apply {
                            addAll(relations.filter { it.key == id })
                        } ?: relations.filter { it.key == id }
                    }
                }
            }
            loadAsync()
        }
    }

    fun toggleTag(
        id: String, tagId: String
    ) {
        val tagIds = _tagsMapFlow.value[id]?.map { it.tagId } ?: emptyList()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (tagIds.contains(tagId)) {
                    TagHelper.deleteTagRelationByKeysTagId(setOf(id), tagId)
                    _tagsMapFlow.value[id] = _tagsMapFlow.value[id]?.filter { it.tagId != tagId } ?: emptyList()
                } else {
                    val relation = DTagRelation(tagId, id, dataType.value.value)
                    TagHelper.addTagRelations(
                        listOf(relation)
                    )
                    _tagsMapFlow.value[id] = _tagsMapFlow.value[id]?.toMutableList()?.apply {
                        add(relation)
                    } ?: listOf(relation)
                }
                loadAsync()
            } catch (ex: Exception) {
                LogCat.e(ex.toString())
            }
        }
    }

    fun showAddDialog() {
        editTagName.value = ""
        editItem.value = null
        tagNameDialogVisible.value = true
    }

    fun showEditDialog(tag: DTag) {
        editTagName.value = tag.name
        editItem.value = tag
        tagNameDialogVisible.value = true
    }
}
