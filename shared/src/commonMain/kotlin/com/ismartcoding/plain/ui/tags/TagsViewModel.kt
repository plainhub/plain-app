package com.ismartcoding.plain.ui.tags

import com.ismartcoding.plain.ui.state.launchSafe

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.IData
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.features.tags.TagsApi
import com.ismartcoding.plain.features.tags.RustContentTags
import com.ismartcoding.plain.models.Tag
import com.ismartcoding.plain.models.TagRelation
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.ui.components.LoadingHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TagsViewModel internal constructor(internal val api: TagsApi = RustContentTags) : ViewModel() {
    private val _itemsFlow = MutableStateFlow<List<Tag>>(emptyList())
    val itemsFlow: StateFlow<List<Tag>> = _itemsFlow
    private val _tagsMapFlow = MutableStateFlow(mutableMapOf<String, List<TagRelation>>())
    val tagsMapFlow = _tagsMapFlow.asStateFlow()
    var showLoading = mutableStateOf(true)
    var tagNameDialogVisible = mutableStateOf(false)
    var editItem = mutableStateOf<Tag?>(null)
    var editTagName = mutableStateOf("")
    var dataType = mutableStateOf(DataType.DEFAULT)

    fun updateTagsMap(map: Map<String, List<TagRelation>>) {
        _tagsMapFlow.value = map.toMutableMap()
    }

    suspend fun loadAsync(keys: Set<String> = emptySet()) = withIO {
        val startTime = TimeHelper.now().toEpochMilliseconds()
        _itemsFlow.value = api.all(dataType.value)
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
        val created = api.create(dataType.value, name)
        _itemsFlow.update { it + created }
        tagNameDialogVisible.value = false
    }

    suspend fun editTagAsync(name: String) = withIO {
        val updated = api.rename(editItem.value!!.id, name)
        _itemsFlow.update { list ->
            list.map { if (it.id == updated.id) updated else it }
        }
        tagNameDialogVisible.value = false
    }

    fun deleteTag(id: String) {
        viewModelScope.launchSafe {
            api.delete(id)
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

    fun showEditDialog(tag: Tag) {
        editTagName.value = tag.name
        editItem.value = tag
        tagNameDialogVisible.value = true
    }

    fun removeFromTags(ids: Set<String>, tagIds: Set<String>) {
        viewModelScope.launchSafe {
            api.edit(dataType.value, ids, emptyList(), tagIds)
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
            api.edit(dataType.value, items.map { it.id }, tagIds, emptyList())
            refreshRelations(items.map { it.id }.toSet())
            loadAsync()
        }
    }

    suspend fun toggleTagAsync(data: IData, tagId: String) = withIO {
        val tagIds = tagsMapFlow.value[data.id]?.map { it.tagId } ?: emptyList()
        if (tagIds.contains(tagId)) {
            api.edit(dataType.value, listOf(data.id), emptyList(), listOf(tagId))
        } else {
            api.edit(dataType.value, listOf(data.id), listOf(tagId), emptyList())
        }
        refreshRelations(setOf(data.id))
        loadAsync()
    }

    internal suspend fun refreshRelations(keys: Set<String>) {
        if (keys.isEmpty()) return
        val fresh = api.relations(keys, dataType.value).groupBy { it.key }
        _tagsMapFlow.update { current -> current.toMutableMap().apply {
            keys.forEach { this[it] = fresh[it].orEmpty() }
        } }
    }
}
