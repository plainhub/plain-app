package com.ismartcoding.plain.ui.notes

import com.ismartcoding.plain.ui.tags.TagsViewModel
import com.ismartcoding.plain.ui.state.ISearchableViewModel
import com.ismartcoding.plain.ui.state.ISelectableViewModel
import com.ismartcoding.plain.ui.state.launchSafe

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.features.notes.NotesClient
import com.ismartcoding.plain.features.notes.NotesApi
import com.ismartcoding.plain.features.tags.TagsApi
import com.ismartcoding.plain.features.tags.RustContentTags
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.models.Note
import com.ismartcoding.plain.models.Tag
import com.ismartcoding.plain.helpers.SearchHelper
import com.ismartcoding.plain.lib.withIO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class NotesViewModel internal constructor(
    val api: NotesApi = NotesClient,
    private val tagsApi: TagsApi = RustContentTags,
) : ISearchableViewModel<Note>, ISelectableViewModel<Note>, ViewModel() {
    companion object {
        val local by lazy { NotesViewModel() }
    }

    private val _itemsFlow = MutableStateFlow<List<Note>>(emptyList())
    override val itemsFlow: StateFlow<List<Note>> = _itemsFlow
    var showLoading = mutableStateOf(true)
    var offset = mutableIntStateOf(0)
    var limit = mutableIntStateOf(200)
    var noMore = mutableStateOf(false)
    var trash = mutableStateOf(false)
    var total = mutableIntStateOf(0)
    var totalTrash = mutableIntStateOf(0)
    var tag = mutableStateOf<Tag?>(null)
    val dataType = DataType.NOTE
    var selectedItem = mutableStateOf<Note?>(null)
    val showTagsDialog = mutableStateOf(false)

    override val showSearchBar = mutableStateOf(false)
    override val searchActive = mutableStateOf(false)
    override val queryText = mutableStateOf("")

    override var selectMode = mutableStateOf(false)
    override val selectedIds = mutableStateListOf<String>()

    suspend fun moreAsync(tagsVM: TagsViewModel) = withIO {
        offset.value += limit.intValue
        val items = api.search(getQuery(), limit.intValue, offset.value)
        _itemsFlow.update { it + items }
        tagsVM.loadMoreAsync(items.map { it.id }.toSet())
        showLoading.value = false
        noMore.value = items.size < limit.intValue
    }

    suspend fun reloadAsync() = withIO {
        offset.intValue = 0
        _itemsFlow.value = api.search(getQuery(), limit.intValue, offset.intValue)
        total.intValue = api.count(getTotalQuery())
        totalTrash.intValue = api.count(getTrashQuery())
        noMore.value = _itemsFlow.value.size < limit.intValue
        showLoading.value = false
    }

    suspend fun loadAsync(tagsVM: TagsViewModel) {
        reloadAsync()
        tagsVM.loadAsync(_itemsFlow.value.map { it.id }.toSet())
    }

    fun trash(tagsVM: TagsViewModel, ids: Set<String>) {
        viewModelScope.launchSafe {
            api.trashAsync(ids)
            loadAsync(tagsVM)
        }
    }

    fun updateItem(item: Note) {
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
        viewModelScope.launchSafe {
            api.restoreAsync(ids)
            loadAsync(tagsVM)
        }
    }

    fun delete(tagsVM: TagsViewModel, ids: Set<String>) {
        viewModelScope.launchSafe {
            api.deleteAsync(ids)
            loadAsync(tagsVM)
        }
    }

    private fun getTotalQuery(): String {
        return "${SearchHelper.buildTextFilter(queryText.value)} trash:false"
    }

    private fun getTrashQuery(): String {
        return "${SearchHelper.buildTextFilter(queryText.value)} trash:true"
    }

    private suspend fun getQuery(): String {
        var query = "${SearchHelper.buildTextFilter(queryText.value)} trash:${trash.value}"
        if (tag.value != null) {
            val tagId = tag.value!!.id
            val ids = tagsApi.keys(tagId)
            query += " ids:${ids.joinToString(",")}"
        }

        return query
    }
}
