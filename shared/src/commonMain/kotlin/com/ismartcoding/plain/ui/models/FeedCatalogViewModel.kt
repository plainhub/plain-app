package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.features.feed.CatalogCategory
import com.ismartcoding.plain.features.feed.CatalogFeed
import com.ismartcoding.plain.features.feed.FeedsCatalog
import com.ismartcoding.plain.features.feed.FeedHelper
import com.ismartcoding.plain.helpers.launchSafe
import com.ismartcoding.plain.lib.withIO

class FeedCatalogViewModel : ViewModel() {
    val categories = mutableStateOf<List<CatalogCategory>>(emptyList())
    val loading = mutableStateOf(true)
    val loadFailed = mutableStateOf(false)
    val busyUrls = mutableStateListOf<String>()

    suspend fun loadAsync() = withIO {
        loading.value = true
        loadFailed.value = try {
            categories.value = FeedsCatalog.loadAsync()
            false
        } catch (e: Exception) {
            true
        }
        loading.value = false
    }

    // Subscribes to a single catalog feed (one-time sync kicked off) or
    // unsubscribes the feed with the same URL, then reloads feedsVM so callers
    // see the updated subscribed set.
    fun toggleSubscribeAsync(feed: CatalogFeed, feedsVM: FeedsViewModel) {
        if (busyUrls.contains(feed.url)) return
        busyUrls.add(feed.url)
        viewModelScope.launchSafe {
            try {
                val existing = FeedHelper.getByUrl(feed.url)
                if (existing == null) {
                    val id = FeedHelper.addAsync {
                        this.url = feed.url
                        this.name = feed.name
                    }
                    FeedHelper.fetchOneTime(id)
                } else {
                    feedsVM.deleteAsync(setOf(existing.id))
                }
                feedsVM.loadAsync(withCount = true)
            } finally {
                busyUrls.remove(feed.url)
            }
        }
    }
}
