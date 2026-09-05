package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.features.feed.CatalogCategory
import com.ismartcoding.plain.features.feed.FeedsCatalog
import com.ismartcoding.plain.features.feed.FeedHelper
import com.ismartcoding.plain.helpers.launchSafe
import com.ismartcoding.plain.lib.withIO

class FeedCatalogViewModel : ViewModel() {
    val categories = mutableStateOf<List<CatalogCategory>>(emptyList())
    val loading = mutableStateOf(true)
    val loadFailed = mutableStateOf(false)
    val subscribing = mutableStateOf(false)
    val selectedUrls = mutableStateListOf<String>()

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

    fun toggle(url: String) {
        if (selectedUrls.contains(url)) selectedUrls.remove(url) else selectedUrls.add(url)
    }

    fun selectAll(urls: List<String>) {
        selectedUrls.clear()
        selectedUrls.addAll(urls)
    }

    fun clearSelection() = selectedUrls.clear()

    // Adds each selected catalog feed that is not already subscribed, kicks a
    // one-time sync per added feed, then reloads feedsVM so callers see the
    // updated subscribed set. Reports how many feeds were actually added.
    fun subscribeAsync(feedsVM: FeedsViewModel, onDone: (Int) -> Unit) {
        if (subscribing.value) return
        subscribing.value = true
        viewModelScope.launchSafe {
            val all = categories.value.flatMap { it.feeds }.associateBy { it.url }
            var added = 0
            selectedUrls.toList().forEach { url ->
                if (FeedHelper.getByUrl(url) == null) {
                    val feed = all[url] ?: return@forEach
                    val id = FeedHelper.addAsync {
                        this.url = feed.url
                        this.name = feed.name
                    }
                    FeedHelper.fetchOneTime(id)
                    added++
                }
            }
            feedsVM.loadAsync(withCount = true)
            selectedUrls.clear()
            subscribing.value = false
            onDone(added)
        }
    }
}
