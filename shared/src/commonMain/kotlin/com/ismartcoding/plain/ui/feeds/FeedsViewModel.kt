package com.ismartcoding.plain.ui.feeds


import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.ui.state.ISelectableViewModel
import com.ismartcoding.plain.ui.state.launchSafe

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.models.Feed
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.error
import com.ismartcoding.plain.i18n.already_added
import kotlinx.coroutines.CancellationException
import com.ismartcoding.plain.i18n.invalid_url
import com.ismartcoding.plain.models.FeedPreview
import com.ismartcoding.plain.ui.i18n.LocaleHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class FeedsViewModel internal constructor(val source: ContentSource = ContentSource.Local) : ISelectableViewModel<Feed>, ViewModel() {
    val syncState = if (source is ContentSource.Remote) FeedSyncStateStore(source.feeds) else FeedWorkerState
    private val _itemsFlow = MutableStateFlow<List<Feed>>(emptyList())
    override val itemsFlow: StateFlow<List<Feed>> = _itemsFlow
    var showLoading = mutableStateOf(true)
    var showAddDialog = mutableStateOf(false)
    var showEditDialog = mutableStateOf(false)
    var selectedItem = mutableStateOf<Feed?>(null)
    internal var editId = mutableStateOf("")
    var editUrl = mutableStateOf("")
    var editName = mutableStateOf("")
    var editFetchContent = mutableStateOf(false)
    var editUrlError = mutableStateOf("")
    var feedPreview = mutableStateOf<FeedPreview?>(null)

    override var selectMode = mutableStateOf(false)
    override val selectedIds = mutableStateListOf<String>()

    fun loadAsync(withCount: Boolean = false) {
        viewModelScope.launchSafe {
            _itemsFlow.value = source.feeds.getAll(withCount)
            showLoading.value = false
        }
    }

    fun updateFetchContent(id: String, value: Boolean) {
        viewModelScope.launchSafe {
            source.feeds.update(id, fetchContent = value)
        }
    }

    /** Reloads the feed shown in ViewFeedBottomSheet so its sync status card stays fresh. */
    fun refreshSelectedItemAsync() {
        val id = selectedItem.value?.id ?: return
        viewModelScope.launchSafe {
            val fresh = source.feeds.getById(id) ?: return@launchSafe
            fresh.entryCount = selectedItem.value?.entryCount ?: 0
            selectedItem.value = fresh
        }
    }

    fun delete(ids: Set<String>) {
        viewModelScope.launchSafe { deleteAsync(ids) }
    }

    suspend fun deleteAsync(ids: Set<String>) {
        source.feeds.deleteAsync(ids)
        _itemsFlow.update { it.filterNot { i -> ids.contains(i.id) } }
    }

    fun add() {
        editUrlError.value = ""
        viewModelScope.launchSafe {
            source.feeds.create(editUrl.value, editName.value, editFetchContent.value)
            loadAsync(withCount = true)
            showAddDialog.value = false
        }
    }

    fun fetchChannel() {
        editUrlError.value = ""
        val url = editUrl.value
        viewModelScope.launchSafe {
            try {
                val preview = source.feeds.preview(url)
                if (!showAddDialog.value || editUrl.value != url) return@launchSafe
                feedPreview.value = preview
                editName.value = preview.title
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                if (showAddDialog.value && editUrl.value == url) editUrlError.value = feedErrorText(e)
            }
        }
    }

    fun edit() {
        editUrlError.value = ""
        viewModelScope.launchSafe {
            try {
                source.feeds.update(editId.value, name = editName.value, url = editUrl.value, fetchContent = editFetchContent.value)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                editUrlError.value = feedErrorText(e)
                return@launchSafe
            }
            loadAsync(withCount = true)
            showEditDialog.value = false
        }
    }

    private suspend fun feedErrorText(error: Exception): String = when (error.message) {
        "feed_already_exists" -> LocaleHelper.getStringAsync(Res.string.already_added)
        "invalid_feed_url" -> LocaleHelper.getStringAsync(Res.string.invalid_url)
        else -> error.message ?: LocaleHelper.getStringAsync(Res.string.error)
    }

    fun showAddDialog() {
        feedPreview.value = null
        editUrlError.value = ""
        editUrl.value = ""
        editName.value = ""
        editFetchContent.value = false
        showAddDialog.value = true
    }

    fun showEditDialog(item: Feed) {
        editUrlError.value = ""
        editId.value = item.id
        editUrl.value = item.url
        editName.value = item.name
        editFetchContent.value = item.fetchContent
        showEditDialog.value = true
    }

}
