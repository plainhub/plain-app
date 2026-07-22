package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.platform.countMedia
import com.ismartcoding.plain.platform.deleteMedia
import com.ismartcoding.plain.platform.restoreMedia
import com.ismartcoding.plain.platform.searchMedia
import com.ismartcoding.plain.platform.trashMedia
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

abstract class BaseMediaViewModel<T : IData> : ISearchableViewModel<T>, ViewModel() {
    internal val _itemsFlow = MutableStateFlow<List<T>>(emptyList())
    val itemsFlow: StateFlow<List<T>> = _itemsFlow
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

    /** When true, folders are displayed as a list instead of a grid (e.g. audio). */
    open val showFoldersAsList: Boolean = false

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

    internal open suspend fun getQuery(): String {
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

    suspend fun moreAsync(tagsVM: TagsViewModel) = withIO {
        offset.intValue += limit.intValue
        val items = searchMediaAsync(getQuery())
        _itemsFlow.update { it + items }
        tagsVM.loadMoreAsync(items.map { it.id }.toSet())
        noMore.value = items.size < limit.intValue
        showLoading.value = false
    }

    open suspend fun loadAsync(tagsVM: TagsViewModel) = withIO {
        offset.intValue = 0
        _itemsFlow.value = searchMediaAsync(getQuery())
        tagsVM.loadAsync(_itemsFlow.value.map { it.id }.toSet())
        total.intValue = countMediaAsync(getTotalQuery())
        totalTrash.intValue = countMediaAsync(getTrashQuery())
        noMore.value = _itemsFlow.value.size < limit.intValue
        showLoading.value = false
    }

    fun trash(tagsVM: TagsViewModel, ids: Set<String>) {
        trashItems(tagsVM, ids)
    }

    fun restore(tagsVM: TagsViewModel, ids: Set<String>) {
        restoreItems(tagsVM, ids)
    }

    open fun delete(tagsVM: TagsViewModel, ids: Set<String>) {
        viewModelScope.launchSafe {
            DialogHelper.showLoading()
            TagHelper.deleteTagRelationByKeys(ids, dataType)
            deleteMedia(dataType, ids, trash.value)
            loadAsync(tagsVM)
            DialogHelper.hideLoading()
        }
    }

    fun trashItems(
        tagsVM: TagsViewModel, ids: Set<String>,
    ) {
        viewModelScope.launchSafe {
            DialogHelper.showLoading()
            TagHelper.deleteTagRelationByKeys(ids, dataType)
            trashMedia(dataType, ids)
            loadAsync(tagsVM)
            DialogHelper.hideLoading()
            _itemsFlow.update { it.filterNot { i -> ids.contains(i.id) } }
        }
    }

    fun restoreItems(
        tagsVM: TagsViewModel, ids: Set<String>,
    ) {
        viewModelScope.launchSafe {
            DialogHelper.showLoading()
            restoreMedia(dataType, ids)
            loadAsync(tagsVM)
            DialogHelper.hideLoading()
            _itemsFlow.update { it.filterNot { i -> ids.contains(i.id) } }
        }
    }

    suspend fun countMediaAsync(
        query: String,
    ): Int = withIO {
        countMedia(dataType, query)
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun searchMediaAsync(
        query: String,
    ): List<T> = withIO {
        searchMedia(dataType, query, limit.intValue, offset.intValue, sortBy.value) as List<T>
    }
}
