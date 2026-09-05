package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.db.DFeed
import com.ismartcoding.plain.db.DFeedEntry
import com.ismartcoding.plain.features.feed.FeedEntryHelper
import com.ismartcoding.plain.features.feed.FeedHelper
import com.ismartcoding.plain.helpers.launchSafe
import com.ismartcoding.plain.platform.fetchContentAsync

/**
 * Carries the filtered entry list from FeedEntriesPage into FeedEntryPage so
 * swiping left/right walks the same filtered set, and caches entry rows plus
 * prefetched full content so neighbor pages render instantly.
 */
class FeedEntryPagerViewModel : ViewModel() {
    val ids = mutableStateListOf<String>()

    private val entries = HashMap<String, DFeedEntry>()
    private val contents = HashMap<String, String>()
    private val feeds = HashMap<String, DFeed?>()
    private val fetchingIds = mutableSetOf<String>()

    fun setup(list: List<String>) {
        if (list == ids.toList()) return
        ids.clear()
        ids.addAll(list)
    }

    suspend fun entryAsync(id: String): DFeedEntry? {
        entries[id]?.let { return it }
        val m = FeedEntryHelper.getAsync(id) ?: return null
        entries[id] = m
        return m
    }

    suspend fun feedAsync(feedId: String): DFeed? = feeds.getOrPut(feedId) { FeedHelper.getById(feedId) }

    fun cachedContent(id: String): String = contents[id] ?: entries[id]?.content ?: ""

    fun cacheContent(id: String, content: String) {
        if (content.isNotEmpty()) contents[id] = content
    }

    // Cache DB rows for ±2 neighbors so a swipe needs no I/O, and prefetch the
    // full web content of the ±1 neighbors when the feed has fetchContent
    // enabled and no full content is stored yet.
    fun preloadAroundAsync(id: String) {
        viewModelScope.launchSafe {
            val i = ids.indexOf(id)
            if (i < 0) return@launchSafe
            val rowNeighbors = listOfNotNull(ids.getOrNull(i - 2), ids.getOrNull(i - 1), ids.getOrNull(i + 1), ids.getOrNull(i + 2))
            rowNeighbors.forEach { neighbor -> entryAsync(neighbor) }
            listOfNotNull(ids.getOrNull(i - 1), ids.getOrNull(i + 1)).forEach { neighbor ->
                val m = entries[neighbor] ?: return@forEach
                if (m.content.isNotEmpty()) return@forEach
                val feed = feedAsync(m.feedId)
                if (feed?.fetchContent != true) return@forEach
                if (!fetchingIds.add(neighbor)) return@forEach
                try {
                    m.fetchContentAsync()
                    cacheContent(neighbor, m.content)
                } finally {
                    fetchingIds.remove(neighbor)
                }
            }
        }
    }
}
