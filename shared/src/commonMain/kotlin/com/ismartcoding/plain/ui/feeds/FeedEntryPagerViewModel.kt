package com.ismartcoding.plain.ui.feeds

import com.ismartcoding.plain.rust.graphql.ContentSource
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.models.Feed
import com.ismartcoding.plain.models.FeedEntry
import com.ismartcoding.plain.ui.state.launchSafe
import com.ismartcoding.plain.features.feeds.fetchContentAsync
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * Carries the filtered entry list from FeedEntriesPage into FeedEntryPage so
 * swiping left/right walks the same filtered set, and caches entry rows plus
 * prefetched full content so neighbor pages render instantly.
 */
class FeedEntryPagerViewModel internal constructor(val source: ContentSource = ContentSource.Local) : ViewModel() {
    val ids = mutableStateListOf<String>()

    private val entries = HashMap<String, FeedEntry>()
    private val contents = HashMap<String, String>()
    private val feeds = HashMap<String, Feed?>()
    private val fetchingIds = mutableSetOf<String>()

    fun setup(list: List<String>) {
        if (list == ids.toList()) return
        ids.clear()
        ids.addAll(list)
    }

    suspend fun entryAsync(id: String): FeedEntry? {
        entries[id]?.let { return it }
        val m = source.feedEntries.getAsync(id) ?: return null
        entries[id] = m
        return m
    }

    suspend fun feedAsync(feedId: String): Feed? = feeds.getOrPut(feedId) { source.feeds.getById(feedId) }

    /** Mark an entry read while paging: persist immediately, keep the row cache in sync. */
    fun markRead(id: String) {
        entries[id]?.let { it.read = true }
        viewModelScope.launchSafe { source.feedEntries.markReadAsync(setOf(id)) }
    }

    fun cachedContent(id: String): String = contents[id] ?: entries[id]?.content ?: ""

    fun cacheContent(id: String, content: String) {
        if (content.isNotEmpty()) contents[id] = content
    }

    private var latestPreloadId: String? = null

    // Cache DB rows for ±2 neighbors so a swipe needs no I/O, and prefetch the
    // full web content of the ±1 neighbors when the feed has fetchContent
    // enabled and no full content is stored yet. The content prefetch is heavy
    // (network fetch + readability extraction + image import), so it only starts
    // after the reader has settled on a page for a moment — a swipe burst must
    // never compete with frame rendering.
    fun preloadAroundAsync(id: String) {
        latestPreloadId = id
        viewModelScope.launchSafe {
            val i = ids.indexOf(id)
            if (i < 0) return@launchSafe
            val rowNeighbors = listOfNotNull(ids.getOrNull(i - 2), ids.getOrNull(i - 1), ids.getOrNull(i + 1), ids.getOrNull(i + 2))
            rowNeighbors.forEach { neighbor -> entryAsync(neighbor) }
            delay(1000)
            if (latestPreloadId != id) return@launchSafe
            listOfNotNull(ids.getOrNull(i - 1), ids.getOrNull(i + 1)).forEach { neighbor ->
                val m = entries[neighbor] ?: return@forEach
                if (m.content.isNotEmpty()) return@forEach
                val feed = feedAsync(m.feedId)
                if (feed?.fetchContent != true) return@forEach
                if (!fetchingIds.add(neighbor)) return@forEach
                try {
                    m.fetchContentAsync()
                    cacheContent(neighbor, m.content)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    LogCat.e("Preload feed entry content", error)
                } finally {
                    fetchingIds.remove(neighbor)
                }
            }
        }
    }
}
