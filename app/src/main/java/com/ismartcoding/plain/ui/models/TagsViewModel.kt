package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.db.DTagRelation
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.ui.helpers.LoadingHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@OptIn(androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi::class)
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

    internal fun updateTagsMap(map: Map<String, List<DTagRelation>>) {
        _tagsMapFlow.value = map.toMutableMap()
    }

    suspend fun loadAsync(keys: Set<String> = emptySet()) = withIO {
        val startTime = System.currentTimeMillis()
        val tagCountMap = TagHelper.count(dataType.value).associate { it.id to it.count }
        _itemsFlow.value = TagHelper.getAll(dataType.value).map { tag ->
            tag.count = tagCountMap[tag.id] ?: 0
            tag
        }
        if (keys.isNotEmpty()) {
            _tagsMapFlow.value += TagHelper.getTagRelationsByKeysMap(keys, dataType.value).toMutableMap()
        }
        LoadingHelper.ensureMinimumLoadingTime(
            viewModel = this@TagsViewModel,
            startTime = startTime,
            updateLoadingState = { isLoading -> showLoading.value = isLoading }
        )
    }

    fun loadMoreAsync(keys: Set<String>) {
        if (keys.isNotEmpty()) {
            launchSafe {
                _tagsMapFlow.value += TagHelper.getTagRelationsByKeysMap(keys, dataType.value)
            }
        }
    }

    suspend fun addTagAsync(name: String) = withIO {
        val id = TagHelper.addOrUpdate("") {
            this.name = name
            type = dataType.value.value
        }
        _itemsFlow.update { it + DTag(id).apply {
            this.name = name
            type = dataType.value.value
        } }
        tagNameDialogVisible.value = false
    }

    suspend fun editTagAsync(name: String) = withIO {
        val id = TagHelper.addOrUpdate(editItem.value!!.id) {
            this.name = name
        }
        _itemsFlow.update { list ->
            list.map { if (it.id == id) it.apply { this.name = name } else it }
        }
        tagNameDialogVisible.value = false
    }

    fun deleteTag(id: String) {
        launchSafe {
            TagHelper.deleteTagRelationsByTagId(id)
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
}
