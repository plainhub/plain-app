package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.db.IData
import com.ismartcoding.plain.data.TagRelationStub
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.db.DTagRelation
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.ui.helpers.LoadingHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TagsViewModel : ViewModel() {
    private val _itemsFlow = MutableStateFlow<List<DTag>>(emptyList())
    val itemsFlow: StateFlow<List<DTag>> = _itemsFlow
    private val _tagsMapFlow = MutableStateFlow(mutableMapOf<String, List<DTagRelation>>())
    val tagsMapFlow = _tagsMapFlow.asStateFlow()
    var showLoading = mutableStateOf(true)
    var tagNameDialogVisible = mutableStateOf(false)
    var editItem = mutableStateOf<DTag?>(null)
    var editTagName = mutableStateOf("")
    var dataType = mutableStateOf(DataType.DEFAULT)

    fun updateTagsMap(map: Map<String, List<DTagRelation>>) {
        _tagsMapFlow.value = map.toMutableMap()
    }

    suspend fun loadAsync(keys: Set<String> = emptySet()) = withIO {
        val startTime = TimeHelper.now().toEpochMilliseconds()
        _itemsFlow.value = TagHelper.getAll(dataType.value)
        if (keys.isNotEmpty()) {
            refreshRelations(keys)
        }
        LoadingHelper.ensureMinimumLoadingTime(
            viewModel = this@TagsViewModel,
            startTime = startTime,
            updateLoadingState = { isLoading -> showLoading.value = isLoading }
        )
    }

    fun loadMoreAsync(keys: Set<String>) {
        if (keys.isNotEmpty()) {
            viewModelScope.launchSafe {
                refreshRelations(keys)
            }
        }
    }

    suspend fun addTagAsync(name: String) = withIO {
        val id = TagHelper.addOrUpdate("") {
            this.name = name
            type = dataType.value.value
        }
        val created = checkNotNull(TagHelper.get(id))
        _itemsFlow.update { it + created }
        tagNameDialogVisible.value = false
    }

    suspend fun editTagAsync(name: String) = withIO {
        val id = TagHelper.addOrUpdate(editItem.value!!.id) {
            this.name = name
        }
        _itemsFlow.update { list ->
            list.map { if (it.id == id) it.copy(name = name) else it }
        }
        tagNameDialogVisible.value = false
    }

    fun deleteTag(id: String) {
        viewModelScope.launchSafe {
            TagHelper.delete(id)
            _itemsFlow.update { it.filterNot { i -> i.id == id } }
            for (key in _tagsMapFlow.value.keys) {
                _tagsMapFlow.value[key] = _tagsMapFlow.value[key]?.filter { it.tagId != id } ?: emptyList()
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

    fun removeFromTags(ids: Set<String>, tagIds: Set<String>) {
        viewModelScope.launchSafe {
            TagHelper.deleteTagRelationByKeysTagIds(ids, tagIds)
            for (id in ids) {
                tagsMapFlow.value.toMutableMap().let { map ->
                    map[id] = map[id]?.filter { !tagIds.contains(it.tagId) } ?: emptyList()
                    updateTagsMap(map)
                }
            }
            loadAsync()
        }
    }

    fun addToTags(items: List<IData>, tagIds: Set<String>) {
        viewModelScope.launchSafe {
            TagHelper.addTagRelations(items.flatMap { item ->
                tagIds.map { TagRelationStub.create(item).toTagRelation(it, dataType.value) }
            })
            refreshRelations(items.map { it.id }.toSet())
            loadAsync()
        }
    }

    suspend fun toggleTagAsync(data: IData, tagId: String) = withIO {
        val tagIds = tagsMapFlow.value[data.id]?.map { it.tagId } ?: emptyList()
        if (tagIds.contains(tagId)) {
            TagHelper.deleteTagRelationByKeysTagId(setOf(data.id), tagId)
        } else {
            TagHelper.addTagRelations(listOf(TagRelationStub.create(data).toTagRelation(tagId, dataType.value)))
        }
        refreshRelations(setOf(data.id))
        loadAsync()
    }

    private suspend fun refreshRelations(keys: Set<String>) {
        val fresh = TagHelper.getTagRelationsByKeysMap(keys, dataType.value)
        _tagsMapFlow.update { current -> current.toMutableMap().apply {
            keys.forEach { this[it] = fresh[it].orEmpty() }
        } }
    }
}
