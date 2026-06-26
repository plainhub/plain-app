package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.helpers.launchSafe
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.db.DNote
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.NoteHelper
import com.ismartcoding.plain.features.TagHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class NotesViewModel : ISearchableViewModel<DNote>, ISelectableViewModel<DNote>, ViewModel() {
    private val _itemsFlow = MutableStateFlow<List<DNote>>(emptyList())
    override val itemsFlow: StateFlow<List<DNote>> = _itemsFlow
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

    suspend fun moreAsync(tagsVM: TagsViewModel) = withIO {
        offset.value += limit.intValue
        val items = NoteHelper.search(getQuery(), limit.intValue, offset.value)
        _itemsFlow.update { it + items }
        tagsVM.loadMoreAsync(items.map { it.id }.toSet())
        showLoading.value = false
        noMore.value = items.size < limit.intValue
    }

    suspend fun loadAsync(tagsVM: TagsViewModel) = withIO {
        offset.intValue = 0
        val query = getQuery()
        _itemsFlow.value = NoteHelper.search(query, limit.intValue, offset.value)
        tagsVM.loadAsync(_itemsFlow.value.map { it.id }.toSet())
        total.intValue = NoteHelper.count(getTotalQuery())
        totalTrash.intValue = NoteHelper.count(getTrashQuery())
        noMore.value = _itemsFlow.value.size < limit.intValue
        showLoading.value = false
    }

    fun trash(tagsVM: TagsViewModel, ids: Set<String>) {
        launchSafe {
            TagHelper.deleteTagRelationByKeys(ids, dataType)
            NoteHelper.trashAsync(ids)
            loadAsync(tagsVM)
        }
    }

    fun updateItem(item: DNote) {
        _itemsFlow.update {
            val index = it.indexOfFirst { i -> i.id == item.id }
            if (index != -1) {
                it.toMutableList().also { list -> list[index] = item }
            } else {
                listOf(item) + it
            }
        }
    }

    fun restore(tagsVM: TagsViewModel, ids: Set<String>) {
        launchSafe {
            TagHelper.deleteTagRelationByKeys(ids, dataType)
            NoteHelper.restoreAsync(ids)
            loadAsync(tagsVM)
        }
    }

    fun delete(tagsVM: TagsViewModel, ids: Set<String>) {
        launchSafe {
            TagHelper.deleteTagRelationByKeys(ids, dataType)
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

    private suspend fun getQuery(): String {
        var query = "${queryText.value} trash:${trash.value}"
        if (tag.value != null) {
            val tagId = tag.value!!.id
            val ids = TagHelper.getKeysByTagId(tagId)
            query += " ids:${ids.joinToString(",")}"
        }

        return query
    }
}
