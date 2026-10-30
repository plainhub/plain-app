package com.ismartcoding.plain.ui.feeds


import com.ismartcoding.plain.rust.graphql.ContentSource
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.ui.state.launchSafe
import com.ismartcoding.plain.lib.withIO

class FeedCatalogViewModel internal constructor(val source: ContentSource = ContentSource.Local) : ViewModel() {
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

    fun toggleSubscribeAsync(feed: CatalogFeed, feedsVM: FeedsViewModel) {
        if (busyUrls.contains(feed.url)) return
        busyUrls.add(feed.url)
        viewModelScope.launchSafe {
            try {
                val existing = feedsVM.itemsFlow.value.firstOrNull { it.url == feed.url }
                if (existing == null) {
                    source.feeds.create(feed.url, feed.name)
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
