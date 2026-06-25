package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.audio.AudioMediaStoreHelper
import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.docs.DocMediaStoreHelper
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.features.media.ImageMediaStoreHelper
import com.ismartcoding.plain.features.media.VideoMediaStoreHelper
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

    suspend fun moreAsync(context: Context, tagsVM: TagsViewModel) = withIO {
        offset.intValue += limit.intValue
        val items = searchMediaAsync(context, getQuery())
        _itemsFlow.update { it + items }
        tagsVM.loadMoreAsync(items.map { it.id }.toSet())
        noMore.value = items.size < limit.intValue
        showLoading.value = false
    }

    open suspend fun loadAsync(context: Context, tagsVM: TagsViewModel) = withIO {
        offset.intValue = 0
        _itemsFlow.value = searchMediaAsync(context, getQuery())
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

    fun trashItems(
        context: Context, tagsVM: TagsViewModel, ids: Set<String>,
    ) {
        launchSafe {
            DialogHelper.showLoading()
            TagHelper.deleteTagRelationByKeys(ids, dataType)
            when (dataType) {
                DataType.AUDIO -> AudioMediaStoreHelper.trashByIdsAsync(context, ids)
                DataType.DOC -> DocMediaStoreHelper.trashByIdsAsync(context, ids)
                DataType.IMAGE -> ImageMediaStoreHelper.trashByIdsAsync(context, ids)
                DataType.VIDEO -> VideoMediaStoreHelper.trashByIdsAsync(context, ids)
                else -> {}
            }
            loadAsync(context, tagsVM)
            DialogHelper.hideLoading()
            _itemsFlow.update { it.filterNot { i -> ids.contains(i.id) } }
        }
    }

    fun restoreItems(
        context: Context, tagsVM: TagsViewModel, ids: Set<String>,
    ) {
        launchSafe {
            DialogHelper.showLoading()
            when (dataType) {
                DataType.AUDIO -> AudioMediaStoreHelper.restoreByIdsAsync(context, ids)
                DataType.DOC -> DocMediaStoreHelper.restoreByIdsAsync(context, ids)
                DataType.IMAGE -> ImageMediaStoreHelper.restoreByIdsAsync(context, ids)
                DataType.VIDEO -> VideoMediaStoreHelper.restoreByIdsAsync(context, ids)
                else -> {}
            }
            loadAsync(context, tagsVM)
            DialogHelper.hideLoading()
            _itemsFlow.update { it.filterNot { i -> ids.contains(i.id) } }
        }
    }

    suspend fun countMediaAsync(
        context: Context, query: String,
    ): Int = withIO {
        when (dataType) {
            DataType.AUDIO -> AudioMediaStoreHelper.countAsync(context, query)
            DataType.DOC -> DocMediaStoreHelper.countAsync(context, query)
            DataType.IMAGE -> ImageMediaStoreHelper.countAsync(context, query)
            DataType.VIDEO -> VideoMediaStoreHelper.countAsync(context, query)
            else -> 0
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun searchMediaAsync(
        context: Context, query: String,
    ): List<T> = withIO {
        when (dataType) {
            DataType.AUDIO -> AudioMediaStoreHelper.searchAsync(context, query, limit.intValue, offset.intValue, sortBy.value)
            DataType.DOC -> DocMediaStoreHelper.searchAsync(context, query, limit.intValue, offset.intValue, sortBy.value)
            DataType.IMAGE -> ImageMediaStoreHelper.searchAsync(context, query, limit.intValue, offset.intValue, sortBy.value)
            DataType.VIDEO -> VideoMediaStoreHelper.searchAsync(context, query, limit.intValue, offset.intValue, sortBy.value)
            else -> emptyList()
        } as List<T>
    }
}
