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
import androidx.lifecycle.viewmodel.compose.saveable
import com.ismartcoding.lib.extensions.scanFileByConnection
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.data.DImage
import com.ismartcoding.plain.features.ImageMediaStoreHelper
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

@OptIn(SavedStateHandleSaveableApi::class)
class ImagesViewModel(private val savedStateHandle: SavedStateHandle) : ISelectableViewModel<DImage>, ViewModel() {
    private val _itemsFlow = MutableStateFlow(mutableStateListOf<DImage>())
    override val itemsFlow: StateFlow<List<DImage>> get() = _itemsFlow
    val showLoading = mutableStateOf(true)
    val offset = mutableIntStateOf(0)
    val limit = mutableIntStateOf(1000)
    val noMore = mutableStateOf(false)
    var trash = mutableStateOf(false)
    var total = mutableIntStateOf(0)
    var totalTrash = mutableIntStateOf(0)
    var tag = mutableStateOf<DTag?>(null)
    val bucket = mutableStateOf<DMediaBucket?>(null)
    val dataType = DataType.IMAGE
    var queryText by savedStateHandle.saveable { mutableStateOf("") }
    val search = mutableStateOf(false)
    val selectedItem = mutableStateOf<DImage?>(null)
    val sortBy = mutableStateOf(FileSortBy.DATE_DESC)
    val showRenameDialog = mutableStateOf(false)
    val showSortDialog = mutableStateOf(false)

    override var selectMode = mutableStateOf(false)
    override val selectedIds = mutableStateListOf<String>()

    suspend fun moreAsync(context: Context, tagsViewModel: TagsViewModel) {
        offset.value += limit.value
        val items = ImageMediaStoreHelper.search(context, getQuery(), limit.value, offset.value, sortBy.value)
        _itemsFlow.value.addAll(items)
        showLoading.value = false
        noMore.value = items.size < limit.value
    }

    suspend fun loadAsync(context: Context, tagsViewModel: TagsViewModel) {
        offset.value = 0
        _itemsFlow.value = ImageMediaStoreHelper.search(context, getQuery(), limit.value, offset.value, sortBy.value).toMutableStateList()
        tagsViewModel.loadAsync(_itemsFlow.value.map { it.id }.toSet())
        total.value = ImageMediaStoreHelper.count(context, getTotalQuery())
        noMore.value = _itemsFlow.value.size < limit.value
        showLoading.value = false
    }

    fun delete(context: Context, ids: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            DialogHelper.showLoading()
            TagHelper.deleteTagRelationByKeys(ids, dataType)
            ImageMediaStoreHelper.deleteRecordsAndFilesByIds(context, ids)
            DialogHelper.hideLoading()
            _itemsFlow.update {
                it.toMutableStateList().apply {
                    removeIf { i -> ids.contains(i.id) }
                }
            }
        }
    }

    private fun getTotalQuery(): String {
        return "trash:false"
    }

    private fun getTrashQuery(): String {
        return "trash:true"
    }

    private fun getQuery(): String {
        var query = "$queryText trash:${trash.value}"
        if (tag.value != null) {
            val tagId = tag.value!!.id
            val ids = TagHelper.getKeysByTagId(tagId)
            query += " ids:${ids.joinToString(",")}"
        }

        if (bucket.value != null) {
            query += " bucket_id:${bucket.value!!.id}"
        }

        return query
    }
}
