package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DVideo
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.features.video.VideoMediaStoreHelper
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(SavedStateHandleSaveableApi::class)
class VideosViewModel(private val savedStateHandle: SavedStateHandle) :
    ISearchableViewModel<DVideo>,
    ViewModel() {
    private val _itemsFlow = MutableStateFlow(mutableStateListOf<DVideo>())
    val itemsFlow: StateFlow<List<DVideo>> get() = _itemsFlow
    val showLoading = mutableStateOf(true)
    val offset = mutableIntStateOf(0)
    val limit = mutableIntStateOf(1000)
    val noMore = mutableStateOf(false)
    var trash = mutableStateOf(false)
    var total = mutableIntStateOf(0)
    var totalTrash = mutableIntStateOf(0)
    var tag = mutableStateOf<DTag?>(null)
    val bucketId = mutableStateOf<String>("")
    val dataType = DataType.VIDEO
    val selectedItem = mutableStateOf<DVideo?>(null)
    val sortBy = mutableStateOf(FileSortBy.DATE_DESC)
    val showRenameDialog = mutableStateOf(false)
    val showSortDialog = mutableStateOf(false)
    var tabs = mutableStateOf(listOf<VTabData>())

    override val showSearchBar = mutableStateOf(false)
    override val searchActive = mutableStateOf(false)
    override val queryText = mutableStateOf("")

    fun moreAsync(context: Context, tagsViewModel: TagsViewModel) {
        offset.value += limit.value
        val items = VideoMediaStoreHelper.search(context, getQuery(), limit.value, offset.value, sortBy.value)
        _itemsFlow.value.addAll(items)
        tagsViewModel.loadMoreAsync(items.map { it.id }.toSet())
        showLoading.value = false
        noMore.value = items.size < limit.value
    }

    fun loadAsync(context: Context, tagsViewModel: TagsViewModel) {
        offset.value = 0
        _itemsFlow.value = VideoMediaStoreHelper.search(context, getQuery(), limit.value, offset.value, sortBy.value).toMutableStateList()
        refreshTabsAsync(context, tagsViewModel)
        showLoading.value = false
    }

    // for updating tags, delete items
    fun refreshTabsAsync(context: Context, tagsViewModel: TagsViewModel) {
        tagsViewModel.loadAsync(_itemsFlow.value.map { it.id }.toSet())
        total.value = VideoMediaStoreHelper.count(context, getTotalQuery())
        noMore.value = _itemsFlow.value.size < limit.value
        tabs.value = listOf(
            VTabData(LocaleHelper.getString(R.string.all), "all", total.value),
            * tagsViewModel.itemsFlow.value.map { VTabData(it.name, it.id, it.count) }.toTypedArray()
        )
    }

    fun delete(context: Context, tagsViewModel: TagsViewModel, ids: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            DialogHelper.showLoading()
            TagHelper.deleteTagRelationByKeys(ids, dataType)
            VideoMediaStoreHelper.deleteRecordsAndFilesByIds(context, ids)
            refreshTabsAsync(context, tagsViewModel)
            DialogHelper.hideLoading()
            _itemsFlow.update {
                it.toMutableStateList().apply {
                    removeIf { i -> ids.contains(i.id) }
                }
            }
        }
    }

    private fun getTotalQuery(): String {
        var query = "${queryText.value} trash:false"
        if (bucketId.value.isNotEmpty()) {
            query += " bucket_id:${bucketId.value}"
        }
        return query
    }

    private fun getTrashQuery(): String {
        var query = "${queryText.value} trash:true"
        if (bucketId.value.isNotEmpty()) {
            query += " bucket_id:${bucketId.value}"
        }
        return query
    }

    private fun getQuery(): String {
        var query = "${queryText.value} trash:${trash.value}"
        if (tag.value != null) {
            val tagId = tag.value!!.id
            val ids = TagHelper.getKeysByTagId(tagId)
            query += " ids:${ids.joinToString(",")}"
        }

        if (bucketId.value.isNotEmpty()) {
            query += " bucket_id:${bucketId.value}"
        }

        return query
    }
}
