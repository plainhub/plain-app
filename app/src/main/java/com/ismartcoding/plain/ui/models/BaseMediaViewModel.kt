package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.features.media.AudioMediaStoreHelper
import com.ismartcoding.plain.features.media.ImageMediaStoreHelper
import com.ismartcoding.plain.features.media.VideoMediaStoreHelper
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class BaseMediaViewModel<T : IData> : ISearchableViewModel<T>, ViewModel() {
    protected val _itemsFlow = MutableStateFlow(mutableStateListOf<T>())
    val itemsFlow: StateFlow<List<T>> get() = _itemsFlow
    var tag = mutableStateOf<DTag?>(null)
    var trash = mutableStateOf(false)
    var bucketId = mutableStateOf("")
    var showLoading = mutableStateOf(true)
    var hasPermission = mutableStateOf(false)
    val showSortDialog = mutableStateOf(false)
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

    override val showSearchBar = mutableStateOf(false)
    override val searchActive = mutableStateOf(false)
    override val queryText = mutableStateOf("")

    abstract val dataType: DataType

    private fun getTotalQuery(): String {
        var query = "${queryText.value} trash:false"
        if (bucketId.value.isNotEmpty()) {
            query += " bucket_id:${bucketId.value}"
        }
        return query
    }

    protected fun getTrashQuery(): String {
        var query = "${queryText.value} trash:true"
        if (bucketId.value.isNotEmpty()) {
            query += " bucket_id:${bucketId.value}"
        }
        return query
    }

    protected fun getQuery(): String {
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

    suspend fun moreAsync(context: Context, tagsViewModel: TagsViewModel) {
        offset.intValue += limit.intValue
        val items = searchAsync(context, getQuery())
        _itemsFlow.update {
            val mutableList = it.toMutableStateList()
            mutableList.addAll(items)
            mutableList
        }
        tagsViewModel.loadMoreAsync(items.map { it.id }.toSet())
        noMore.value = items.size < limit.intValue
        showLoading.value = false
    }

    suspend fun loadAsync(context: Context, tagsViewModel: TagsViewModel) {
        offset.intValue = 0
        _itemsFlow.value = searchAsync(context, getQuery()).toMutableStateList()
        tagsViewModel.loadAsync(_itemsFlow.value.map { it.id }.toSet())
        total.intValue = countAsync(context, getTotalQuery())
        totalTrash.intValue = countAsync(context, getTrashQuery())
        noMore.value = _itemsFlow.value.size < limit.intValue
        showLoading.value = false
    }

    fun trash(context: Context, tagsVM: TagsViewModel, ids: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            DialogHelper.showLoading()
            TagHelper.deleteTagRelationByKeys(ids, dataType)
            when (dataType) {
                DataType.AUDIO -> AudioMediaStoreHelper.trashByIdsAsync(context, ids)
                DataType.IMAGE -> ImageMediaStoreHelper.trashByIdsAsync(context, ids)
                DataType.VIDEO -> VideoMediaStoreHelper.trashByIdsAsync(context, ids)
                else -> {}
            }
            loadAsync(context, tagsVM)
            DialogHelper.hideLoading()
            _itemsFlow.update {
                it.toMutableStateList().apply {
                    removeIf { i -> ids.contains(i.id) }
                }
            }
        }
    }

    fun restore(context: Context, tagsVM: TagsViewModel, ids: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            DialogHelper.showLoading()
            when (dataType) {
                DataType.AUDIO -> AudioMediaStoreHelper.restoreByIdsAsync(context, ids)
                DataType.IMAGE -> ImageMediaStoreHelper.restoreByIdsAsync(context, ids)
                DataType.VIDEO -> VideoMediaStoreHelper.restoreByIdsAsync(context, ids)
                else -> {}
            }
            loadAsync(context, tagsVM)
            DialogHelper.hideLoading()
            _itemsFlow.update {
                it.toMutableStateList().apply {
                    removeIf { i -> ids.contains(i.id) }
                }
            }
        }
    }

    private suspend fun countAsync(context: Context, query: String): Int {
        return when (dataType) {
            DataType.AUDIO -> {
                AudioMediaStoreHelper.countAsync(context, query)
            }

            DataType.IMAGE -> {
                ImageMediaStoreHelper.countAsync(context, query)
            }

            DataType.VIDEO -> {
                VideoMediaStoreHelper.countAsync(context, query)
            }

            else -> 0
        }
    }

    private suspend fun searchAsync(context: Context, query: String): List<T> {
        return when (dataType) {
            DataType.AUDIO -> {
                AudioMediaStoreHelper.searchAsync(context, query, limit.intValue, offset.intValue, sortBy.value)
            }

            DataType.IMAGE -> {
                ImageMediaStoreHelper.searchAsync(context, query, limit.intValue, offset.intValue, sortBy.value)
            }

            DataType.VIDEO -> {
                VideoMediaStoreHelper.searchAsync(context, query, limit.intValue, offset.intValue, sortBy.value)
            }

            else -> emptyList()
        } as List<T>
    }
} 