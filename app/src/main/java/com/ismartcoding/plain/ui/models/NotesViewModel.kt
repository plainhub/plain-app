package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.saveable
import com.ismartcoding.plain.data.enums.DataType
import com.ismartcoding.plain.db.DNote
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.features.note.NoteHelper
import com.ismartcoding.plain.features.tag.TagHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi::class)
class NotesViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    private val _itemsFlow = MutableStateFlow(mutableStateListOf<DNote>())
    val itemsFlow: StateFlow<List<DNote>> get() = _itemsFlow
    var showLoading = mutableStateOf(true)
    var offset = mutableIntStateOf(0)
    var limit = mutableIntStateOf(50)
    var noMore = mutableStateOf(false)
    var trash = mutableStateOf(false)
    var total = mutableIntStateOf(0)
    var tag = mutableStateOf<DTag?>(null)
    val dataType = DataType.NOTE
    var queryText by savedStateHandle.saveable { mutableStateOf("") }

    var selectMode by savedStateHandle.saveable { mutableStateOf(false) }
    val selectedIds = mutableStateListOf<String>()

    fun moreAsync(tagsViewModel: TagsViewModel) {
        offset.value += limit.value
        val items = NoteHelper.search(getQuery(), limit.value, offset.value)
        _itemsFlow.update {
            val mutableList = it.toMutableStateList()
            mutableList.addAll(items)
            mutableList
        }
        tagsViewModel.loadMoreAsync(items.map { it.id }.toSet())
        showLoading.value = false
        noMore.value = items.size < limit.value
    }

    fun loadAsync(tagsViewModel: TagsViewModel) {
        offset.value = 0
        val query = getQuery()
        _itemsFlow.value = NoteHelper.search(query, limit.value, offset.value).toMutableStateList()
        tagsViewModel.loadAsync(_itemsFlow.value.map { it.id }.toSet())
        total.value = NoteHelper.count(query)
        noMore.value = _itemsFlow.value.size < limit.value
        showLoading.value = false
    }

    fun trash(ids: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            TagHelper.deleteTagRelationByKeys(
                ids,
                DataType.NOTE,
            )
            NoteHelper.trashAsync(ids)
            total.value = NoteHelper.count(getQuery())
            _itemsFlow.update {
                val mutableList = it.toMutableStateList()
                mutableList.removeIf { m -> ids.contains(m.id) }
                mutableList
            }
        }
    }

    fun untrash(ids: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            TagHelper.deleteTagRelationByKeys(
                ids,
                DataType.NOTE,
            )
            NoteHelper.untrashAsync(ids)
            total.value = NoteHelper.count(getQuery())
            _itemsFlow.update {
                val mutableList = it.toMutableStateList()
                mutableList.removeIf { m -> ids.contains(m.id) }
                mutableList
            }
        }
    }

    fun delete(ids: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            TagHelper.deleteTagRelationByKeys(
                ids,
                DataType.NOTE,
            )
            NoteHelper.deleteAsync(ids)
            total.value = NoteHelper.count(getQuery())
            _itemsFlow.update {
                val mutableList = it.toMutableStateList()
                mutableList.removeIf { m -> ids.contains(m.id) }
                mutableList
            }
        }
    }

    private fun getQuery(): String {
        var query = "$queryText trash:${trash.value}"
        if (tag.value != null) {
            val tagId = tag.value!!.id
            val ids = TagHelper.getKeysByTagId(tagId)
            query += " ids:${ids.joinToString(",")}"
        }

        return query
    }

    fun enterSelectMode() {
        selectMode = true
    }

    fun exitSelectMode() {
        selectMode = false
        selectedIds.clear()
    }

    fun toggleSelectMode() {
        selectMode = !selectMode
        selectedIds.clear()
    }

    fun isAllSelected(): Boolean {
        return selectedIds.size == _itemsFlow.value.size
    }

    fun toggleSelectAll() {
        if (isAllSelected()) {
            selectedIds.clear()
        } else {
            selectedIds.clear()
            selectedIds.addAll(_itemsFlow.value.map { it.id })
        }
    }

    fun select(id: String) {
        if (selectedIds.contains(id)) {
            selectedIds.remove(id)
        } else {
            selectedIds.add(id)
        }
    }
}
