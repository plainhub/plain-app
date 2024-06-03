package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.db.DNote
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.features.NoteHelper
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.features.locale.LocaleHelper
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
    private var totalTrash = mutableIntStateOf(0)
    var tag = mutableStateOf<DTag?>(null)
    val dataType = DataType.NOTE
    var selectedItem = mutableStateOf<DNote?>(null)
    var tabs = mutableStateOf(listOf<VTabData>())

    override val showSearchBar = mutableStateOf(false)
    override val searchActive = mutableStateOf(false)
    override val queryText = mutableStateOf("")

    override var selectMode = mutableStateOf(false)
    override val selectedIds = mutableStateListOf<String>()

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
        refreshTabsAsync(tagsViewModel)
        noMore.value = _itemsFlow.value.size < limit.value
        showLoading.value = false
    }

    fun refreshTabsAsync(tagsViewModel: TagsViewModel) {
        tagsViewModel.loadAsync(_itemsFlow.value.map { it.id }.toSet())
        total.value = NoteHelper.count(getTotalQuery())
        totalTrash.value = NoteHelper.count(getTrashQuery())
        tabs.value = listOf(
            VTabData(LocaleHelper.getString(R.string.all), "all", total.value),
            VTabData(LocaleHelper.getString(R.string.trash), "trash", totalTrash.value),
            * tagsViewModel.itemsFlow.value.map { VTabData(it.name, it.id, it.count) }.toTypedArray()
        )
    }

    fun trash(tagsViewModel: TagsViewModel, ids: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            TagHelper.deleteTagRelationByKeys(
                ids,
                dataType,
            )
            NoteHelper.trashAsync(ids)
            refreshTabsAsync(tagsViewModel)
            _itemsFlow.update {
                val mutableList = it.toMutableStateList()
                mutableList.removeIf { m -> ids.contains(m.id) }
                mutableList
            }
        }
    }

    fun updateItem(item: DNote) {
        _itemsFlow.update {
            val mutableList = it.toMutableStateList()
            val index = mutableList.indexOfFirst { i -> i.id == item.id }
            if (index != -1) {
                mutableList.removeAt(index)
                mutableList.add(index, item)
            }  else {
                mutableList.add(0, item)
            }
            mutableList
        }
    }

    fun untrash(tagsViewModel: TagsViewModel, ids: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            TagHelper.deleteTagRelationByKeys(
                ids,
                dataType,
            )
            NoteHelper.untrashAsync(ids)
            refreshTabsAsync(tagsViewModel)
            _itemsFlow.update {
                val mutableList = it.toMutableStateList()
                mutableList.removeIf { m -> ids.contains(m.id) }
                mutableList
            }
        }
    }

    fun delete(tagsViewModel: TagsViewModel, ids: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            TagHelper.deleteTagRelationByKeys(
                ids,
                dataType,
            )
            NoteHelper.deleteAsync(ids)
            refreshTabsAsync(tagsViewModel)
            _itemsFlow.update {
                val mutableList = it.toMutableStateList()
                mutableList.removeIf { m -> ids.contains(m.id) }
                mutableList
            }
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
