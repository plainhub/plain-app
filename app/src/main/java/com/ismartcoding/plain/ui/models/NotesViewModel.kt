package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.db.DNote
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.NoteHelper
import com.ismartcoding.plain.features.TagHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi::class)
class NotesViewModel(private val savedStateHandle: SavedStateHandle) : ISearchableViewModel<DNote>, ISelectableViewModel<DNote>, ViewModel() {
    private val _itemsFlow = MutableStateFlow(mutableStateListOf<DNote>())
    override val itemsFlow: StateFlow<List<DNote>> get() = _itemsFlow
    var showLoading = mutableStateOf(true)
    var offset = mutableIntStateOf(0)
    var limit = mutableIntStateOf(200)
    var noMore = mutableStateOf(false)
    var trash = mutableStateOf(false)
    var total = mutableIntStateOf(0)
    var totalTrash = mutableIntStateOf(0)
    var tag = mutableStateOf<DTag?>(null)
    val dataType = DataType.NOTE
    var selectedItem = mutableStateOf<DNote?>(null)
    val showTagsDialog = mutableStateOf(false)

    override val showSearchBar = mutableStateOf(false)
    override val searchActive = mutableStateOf(false)
    override val queryText = mutableStateOf("")

    override var selectMode = mutableStateOf(false)
    override val selectedIds = mutableStateListOf<String>()

    suspend fun moreAsync(tagsVM: TagsViewModel) {
        offset.value += limit.intValue
        val items = NoteHelper.search(getQuery(), limit.intValue, offset.intValue)
        _itemsFlow.update {
            val mutableList = it.toMutableStateList()
            mutableList.addAll(items)
            mutableList
        }
        tagsVM.loadMoreAsync(items.map { it.id }.toSet())
        showLoading.value = false
        noMore.value = items.size < limit.intValue
    }

    suspend fun loadAsync(tagsVM: TagsViewModel) {
        offset.intValue = 0
        val query = getQuery()
        _itemsFlow.value = NoteHelper.search(query, limit.intValue, offset.intValue).toMutableStateList()
        tagsVM.loadAsync(_itemsFlow.value.map { it.id }.toSet())
        total.intValue = NoteHelper.count(getTotalQuery())
        totalTrash.intValue = NoteHelper.count(getTrashQuery())
        noMore.value = _itemsFlow.value.size < limit.intValue
        showLoading.value = false
    }

    fun trash(tagsVM: TagsViewModel, ids: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            TagHelper.deleteTagRelationByKeys(
                ids,
                dataType,
            )
            NoteHelper.trashAsync(ids)
            loadAsync(tagsVM)
        }
    }

    fun updateItem(item: DNote) {
        _itemsFlow.update {
            val mutableList = it.toMutableStateList()
            val index = mutableList.indexOfFirst { i -> i.id == item.id }
            if (index != -1) {
                mutableList.removeAt(index)
                mutableList.add(index, item)
            } else {
                mutableList.add(0, item)
            }
            mutableList
        }
    }

    fun restore(tagsVM: TagsViewModel, ids: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            TagHelper.deleteTagRelationByKeys(
                ids,
                dataType,
            )
            NoteHelper.restoreAsync(ids)
            loadAsync(tagsVM)
        }
    }

    fun delete(tagsVM: TagsViewModel, ids: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            TagHelper.deleteTagRelationByKeys(
                ids,
                dataType,
            )
            NoteHelper.deleteAsync(ids)
            loadAsync(tagsVM)
        }
    }

    private fun getTotalQuery(): String {
        return "${queryText.value} trash:false"
    }

    private fun getTrashQuery(): String {
        return "${queryText.value} trash:true"
    }

    private fun getQuery(): String {
        var query = "${queryText.value} trash:${trash.value}"
        if (tag.value != null) {
            val tagId = tag.value!!.id
            val ids = TagHelper.getKeysByTagId(tagId)
            query += " ids:${ids.joinToString(",")}"
        }

        return query
    }

}
