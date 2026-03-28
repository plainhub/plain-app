package com.ismartcoding.plain.ui.models

import com.ismartcoding.lib.extensions.isUrl
import com.ismartcoding.lib.rss.model.RssChannel
import com.ismartcoding.plain.R
import com.ismartcoding.plain.db.DFeed
import com.ismartcoding.plain.features.feed.FeedHelper
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.workers.FeedFetchWorker
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal fun FeedsViewModel.add() {
    editUrlError.value = ""
    viewModelScope.launch(Dispatchers.IO) {
        val id = FeedHelper.addAsync {
            this.url = editUrl.value
            this.name = editName.value
            this.fetchContent = editFetchContent.value
        }
        FeedFetchWorker.oneTimeRequest(id)
        loadAsync(withCount = true)
        showAddDialog.value = false
    }
}

internal fun FeedsViewModel.fetchChannel() {
    editUrlError.value = ""
    if (!editUrl.value.isUrl()) {
        editUrlError.value = getString(R.string.invalid_url)
        return
    }
    viewModelScope.launch(Dispatchers.IO) {
        if (FeedHelper.getByUrl(editUrl.value) != null) {
            editUrlError.value = getString(R.string.already_added)
            return@launch
        }
        try {
            rssChannel.value = FeedHelper.fetchAsync(editUrl.value)
            rssChannel.value?.let {
                editName.value = it.title ?: ""
            }
        } catch (e: Exception) {
            editUrlError.value = e.message ?: getString(R.string.error)
        }
    }
}

internal fun FeedsViewModel.edit() {
    editUrlError.value = ""
    if (!editUrl.value.isUrl()) {
        editUrlError.value = getString(R.string.invalid_url)
        return
    }
    viewModelScope.launch(Dispatchers.IO) {
        val a = FeedHelper.getByUrl(editUrl.value)
        if (a != null && a.id != editId.value) {
            editUrlError.value = getString(R.string.already_added)
            return@launch
        }
        FeedHelper.updateAsync(editId.value) {
            this.name = editName.value
            this.url = editUrl.value
            this.fetchContent = editFetchContent.value
        }
        loadAsync(withCount = true)
        showEditDialog.value = false
    }
}

internal fun FeedsViewModel.showAddDialog() {
    rssChannel.value = null
    editUrlError.value = ""
    editUrl.value = ""
    editName.value = ""
    editFetchContent.value = false
    showAddDialog.value = true
}

internal fun FeedsViewModel.showEditDialog(item: DFeed) {
    editUrlError.value = ""
    editId.value = item.id
    editUrl.value = item.url
    editName.value = item.name
    editFetchContent.value = item.fetchContent
    showEditDialog.value = true
}
