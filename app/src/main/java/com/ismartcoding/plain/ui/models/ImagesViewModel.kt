package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DImage
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.media.ImageMediaStoreHelper
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(SavedStateHandleSaveableApi::class)
class ImagesViewModel(private val savedStateHandle: SavedStateHandle) :
    ISearchableViewModel<DImage>,
    ViewModel() {
    private val _itemsFlow = MutableStateFlow(mutableStateListOf<DImage>())
    val itemsFlow: StateFlow<List<DImage>> get() = _itemsFlow
    val showLoading = mutableStateOf(true)
    private var offset = 0
    private val limit = 1000
    val noMore = mutableStateOf(false)
    var trash = mutableStateOf(false)
    var tag = mutableStateOf<DTag?>(null)
    val bucketId = mutableStateOf<String>("")
    val dataType = DataType.IMAGE
    val selectedItem = mutableStateOf<DImage?>(null)
    val sortBy = mutableStateOf(FileSortBy.DATE_DESC)
    val showRenameDialog = mutableStateOf(false)
    val showSortDialog = mutableStateOf(false)
    var tabs = mutableStateOf(listOf<VTabData>())

    override val showSearchBar = mutableStateOf(false)
    override val searchActive = mutableStateOf(false)
    override val queryText = mutableStateOf("")

    suspend fun moreAsync(context: Context, tagsViewModel: TagsViewModel) {
        offset += limit
        val items = ImageMediaStoreHelper.searchAsync(context, getQuery(), limit, offset, sortBy.value)
        _itemsFlow.value.addAll(items)
        tagsViewModel.loadMoreAsync(items.map { it.id }.toSet())
        showLoading.value = false
        noMore.value = items.size < limit
    }

    suspend fun loadAsync(context: Context, tagsViewModel: TagsViewModel) {
        offset = 0
        _itemsFlow.value = ImageMediaStoreHelper.searchAsync(context, getQuery(), limit, offset, sortBy.value).toMutableStateList()
        refreshTabsAsync(context, tagsViewModel)
        noMore.value = _itemsFlow.value.size < limit
        showLoading.value = false
    }

    suspend fun refreshTabsAsync(context: Context, tagsViewModel: TagsViewModel) {
        tagsViewModel.loadAsync(_itemsFlow.value.map { it.id }.toSet())
        val total = ImageMediaStoreHelper.countAsync(context, getTotalQuery())
        tabs.value = listOf(
            VTabData(LocaleHelper.getString(R.string.all), "all", total),
            * tagsViewModel.itemsFlow.value.map { VTabData(it.name, it.id, it.count) }.toTypedArray()
        )
    }

    fun delete(context: Context, tagsViewModel: TagsViewModel, ids: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            DialogHelper.showLoading()
            TagHelper.deleteTagRelationByKeys(ids, dataType)
            ImageMediaStoreHelper.deleteRecordsAndFilesByIdsAsync(context, ids)
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
