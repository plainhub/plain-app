package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.features.file.FileSortBy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

abstract class BaseMediaViewModel<T : IData> : ISearchableViewModel<T>, ViewModel() {
    internal val _itemsFlow = MutableStateFlow(mutableStateListOf<T>())
    val itemsFlow: StateFlow<List<T>> get() = _itemsFlow
    var tag = mutableStateOf<DTag?>(null)
    var trash = mutableStateOf(false)
    var bucketId = mutableStateOf("")
    var showLoading = mutableStateOf(true)
    var hasPermission = mutableStateOf(false)
    var total = mutableIntStateOf(0)
    var totalTrash = mutableIntStateOf(0)
    val showFoldersDialog = mutableStateOf(false)
    val noMore = mutableStateOf(false)
    val offset = mutableIntStateOf(0)
    val limit = mutableIntStateOf(1000)
    val sortBy = mutableStateOf(FileSortBy.DATE_DESC)
    var selectedItem = mutableStateOf<T?>(null)
    val showRenameDialog = mutableStateOf(false)
    val showTagsDialog = mutableStateOf(false)
    val showSortAndBrowseDialog = mutableStateOf(false)

    override val showSearchBar = mutableStateOf(false)
    override val searchActive = mutableStateOf(false)
    override val queryText = mutableStateOf("")

    abstract val dataType: DataType

    internal open fun getTotalQuery(): String {
        var query = "${queryText.value} trash:false"
        if (bucketId.value.isNotEmpty()) {
            query += " bucket_id:${bucketId.value}"
        }
        return query
    }

    internal fun getTrashQuery(): String {
        var query = "${queryText.value} trash:true"
        if (bucketId.value.isNotEmpty()) {
            query += " bucket_id:${bucketId.value}"
        }
        return query
    }

    internal open fun getQuery(): String {
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

    suspend fun moreAsync(context: Context, tagsVM: TagsViewModel) {
        offset.intValue += limit.intValue
        val items = searchMediaAsync(context, getQuery())
        _itemsFlow.update {
            val mutableList = it.toMutableStateList()
            mutableList.addAll(items)
            mutableList
        }
        tagsVM.loadMoreAsync(items.map { it.id }.toSet())
        noMore.value = items.size < limit.intValue
        showLoading.value = false
    }

    open suspend fun loadAsync(context: Context, tagsVM: TagsViewModel) {
        offset.intValue = 0
        _itemsFlow.value = searchMediaAsync(context, getQuery()).toMutableStateList()
        tagsVM.loadAsync(_itemsFlow.value.map { it.id }.toSet())
        total.intValue = countMediaAsync(context, getTotalQuery())
        totalTrash.intValue = countMediaAsync(context, getTrashQuery())
        noMore.value = _itemsFlow.value.size < limit.intValue
        showLoading.value = false
    }

    fun trash(context: Context, tagsVM: TagsViewModel, ids: Set<String>) {
        trashItems(context, tagsVM, ids)
    }

    fun restore(context: Context, tagsVM: TagsViewModel, ids: Set<String>) {
        restoreItems(context, tagsVM, ids)
    }
}
