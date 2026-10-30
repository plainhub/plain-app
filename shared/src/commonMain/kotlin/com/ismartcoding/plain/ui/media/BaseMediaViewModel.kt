package com.ismartcoding.plain.ui.media

import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.ui.tags.TagsViewModel
import com.ismartcoding.plain.ui.state.ISearchableViewModel
import com.ismartcoding.plain.ui.state.launchSafe

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.IData
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.features.files.FileSortBy
import com.ismartcoding.plain.models.Tag
import com.ismartcoding.plain.helpers.SearchHelper
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.media.deleteMedia
import com.ismartcoding.plain.platform.media.restoreMedia
import com.ismartcoding.plain.platform.media.trashMedia
import com.ismartcoding.plain.ui.components.DialogHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

abstract class BaseMediaViewModel<T : IData> internal constructor(val source: ContentSource = ContentSource.Local) : ISearchableViewModel<T>, ViewModel() {
    internal var onMediaChanged: (suspend () -> Unit)? = null

    internal val _itemsFlow = MutableStateFlow<List<T>>(emptyList())
    val itemsFlow: StateFlow<List<T>> = _itemsFlow
    var tag = mutableStateOf<Tag?>(null)
    var trash = mutableStateOf(false)
    var bucketId = mutableStateOf("")
    var showLoading = mutableStateOf(true)
    var hasPermission = mutableStateOf(source is ContentSource.Remote && com.ismartcoding.plain.platform.permissions.Permission.WRITE_EXTERNAL_STORAGE in source.info.permissions)
    var total = mutableIntStateOf(0)
    var totalTrash = mutableIntStateOf(0)
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
        var query = "${SearchHelper.buildTextFilter(queryText.value)} trash:false"
        if (bucketId.value.isNotEmpty()) {
            query += " bucket_id:${bucketId.value}"
        }
        return query
    }

    internal fun getTrashQuery(): String {
        var query = "${SearchHelper.buildTextFilter(queryText.value)} trash:true"
        if (bucketId.value.isNotEmpty()) {
            query += " bucket_id:${bucketId.value}"
        }
        return query
    }

    internal open suspend fun getQuery(): String {
        var query = "${SearchHelper.buildTextFilter(queryText.value)} trash:${trash.value}"
        if (tag.value != null) {
            val tagId = tag.value!!.id
            val ids = source.tags.keys(tagId)
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

    fun share(ids: Collection<String>) {
        viewModelScope.launchSafe {
            com.ismartcoding.plain.platform.system.shareFiles(source.catalog.shareUris(dataType, ids))
        }
    }

    fun openFile(path: String) {
        viewModelScope.launchSafe {
            com.ismartcoding.plain.platform.system.openFileExternal(source.fileForOpen(path))
        }
    }

    fun trash(tagsVM: TagsViewModel, ids: Set<String>) {
        trashItems(tagsVM, ids)
    }

    fun restore(tagsVM: TagsViewModel, ids: Set<String>) {
        restoreItems(tagsVM, ids)
    }

    open fun delete(tagsVM: TagsViewModel, ids: Set<String>) {
        viewModelScope.launchSafe { applyMediaAction(tagsVM) { if (source is ContentSource.Remote) source.mediaActions.run(dataType, com.ismartcoding.plain.features.media.actions.MediaAction.DELETE, ids, trash.value) else deleteMedia(dataType, ids, trash.value) } }
    }

    fun trashItems(tagsVM: TagsViewModel, ids: Set<String>) {
        viewModelScope.launchSafe { applyMediaAction(tagsVM) { if (source is ContentSource.Remote) source.mediaActions.run(dataType, com.ismartcoding.plain.features.media.actions.MediaAction.TRASH, ids) else trashMedia(dataType, ids) } }
    }

    fun restoreItems(tagsVM: TagsViewModel, ids: Set<String>) {
        viewModelScope.launchSafe { applyMediaAction(tagsVM) { if (source is ContentSource.Remote) source.mediaActions.run(dataType, com.ismartcoding.plain.features.media.actions.MediaAction.RESTORE, ids) else restoreMedia(dataType, ids) } }
    }

    private suspend fun applyMediaAction(tagsVM: TagsViewModel, action: suspend () -> Unit) {
        DialogHelper.showLoading()
        try { action() }
        finally {
            try { loadAsync(tagsVM) }
            finally {
                DialogHelper.hideLoading()
                onMediaChanged?.invoke()
            }
        }
    }

    suspend fun countMediaAsync(
        query: String,
    ): Int = withIO {
        source.catalog.count(dataType, query)
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun searchMediaAsync(
        query: String,
    ): List<T> = withIO {
        source.catalog.rows(dataType, query, limit.intValue, offset.intValue, sortBy.value) as List<T>
    }
}
