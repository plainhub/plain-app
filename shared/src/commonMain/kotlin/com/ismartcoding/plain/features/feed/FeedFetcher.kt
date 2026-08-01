package com.ismartcoding.plain.features.feed

import com.ismartcoding.plain.db.DFeed
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.FeedStatusEvent
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.helpers.JsonHelper
import com.ismartcoding.plain.helpers.pmap
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.NetworkType
import com.ismartcoding.plain.platform.fetchContentAsync
import com.ismartcoding.plain.platform.fetchRssChannel
import com.ismartcoding.plain.platform.getNetworkType
import com.ismartcoding.plain.preferences.FeedAutoRefreshOnlyWifiPreference
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.Serializable
import kotlin.collections.chunked
import kotlin.collections.forEach
import kotlin.collections.map

@Serializable
private data class FeedsFetchedPayload(val feedId: String, val error: String)

/**
 * Platform-independent RSS feed fetch engine. Extracted from the Android
 * `FeedFetchWorker` so iOS can reuse the exact same fetch pipeline via a
 * foreground coroutine instead of WorkManager.
 *
 * Status tracking goes through [FeedWorkerState]; completion notifications are
 * broadcast via [WebSocketEvent] of type [EventType.FEEDS_FETCHED].
 */
object FeedFetcher {
    /**
     * Sync a single feed by id. No-op if the feed does not exist or is already
     * in [FeedWorkerStatus.PENDING]. Emits a [WebSocketEvent] on completion.
     */
    suspend fun fetchOne(feedId: String) {
        supervisorScope {
            val feed = FeedHelper.getById(feedId) ?: return@supervisorScope
            if (FeedWorkerState.statusMap[feed.id] == FeedWorkerStatus.PENDING) return@supervisorScope
            syncFeedAsync(feed)
            val payload = FeedsFetchedPayload(feedId, FeedWorkerState.errorMap[feedId] ?: "")
            sendEvent(WebSocketEvent(EventType.FEEDS_FETCHED, JsonHelper.jsonEncode(payload)))
        }
    }

    /**
     * Sync all feeds in parallel (chunked by 16). When [autoRefresh] is true,
     * honors the "only refresh on wifi" preference by skipping when not on wifi.
     * Emits a [WebSocketEvent] with the aggregated error map on completion.
     */
    suspend fun fetchAll(autoRefresh: Boolean = false) {
        supervisorScope {
            if (shouldSkipAutoRefresh(
                    autoRefresh,
                    FeedAutoRefreshOnlyWifiPreference.getAsync(),
                    getNetworkType(),
                )
            ) {
                return@supervisorScope
            }
            val feeds = FeedHelper.getAll()
            if (feeds.isEmpty()) return@supervisorScope
            if (FeedWorkerState.statusMap["all"] == FeedWorkerStatus.PENDING) return@supervisorScope

            setStatusMap("all", FeedWorkerStatus.PENDING)
            feeds.chunked(16).forEach {
                it.map { feed -> async { syncFeedAsync(feed) } }.awaitAll()
            }
            setStatusMap("all", FeedWorkerStatus.COMPLETED)
            val payload = FeedsFetchedPayload("all", FeedWorkerState.errorMap.values.joinToString("\n"))
            sendEvent(WebSocketEvent(EventType.FEEDS_FETCHED, JsonHelper.jsonEncode(payload)))
        }
    }

    private suspend fun syncFeedAsync(feed: DFeed) = withIO {
        setStatusMap(feed.id, FeedWorkerStatus.PENDING)
        try {
            LogCat.d("Syncing feed: ${feed.id}, ${feed.name}, ${feed.url}")
            val syndFeed = fetchRssChannel(feed.url)
            val list = syndFeed.items.map { it.toDFeedEntry(feed.id, feed.url) }
            val inserted = FeedEntryHelper.feedEntryDao.insertListIfNotExist(list)
            if (feed.fetchContent) {
                inserted.chunked(4).forEach {
                    it.pmap { f -> f.fetchContentAsync() }
                }
            }
            FeedWorkerState.errorMap.remove(feed.id)
            setStatusMap(feed.id, FeedWorkerStatus.COMPLETED)
        } catch (ex: Throwable) {
            LogCat.e(ex)
            FeedWorkerState.errorMap[feed.id] = ex.toString()
            setStatusMap(feed.id, FeedWorkerStatus.ERROR)
        }
    }

    private fun setStatusMap(feedId: String, status: FeedWorkerStatus) {
        FeedWorkerState.statusMap[feedId] = status
        sendEvent(FeedStatusEvent(feedId, status))
    }
}

/**
 * Decide whether an auto-refresh cycle should be skipped based on the
 * "only refresh on wifi" preference. Returns true (skip) when auto-refresh
 * is enabled, the wifi-only preference is on, and the current network is
 * not wifi. Manual refreshes ([autoRefresh] == false) are never skipped.
 */
internal fun shouldSkipAutoRefresh(
    autoRefresh: Boolean,
    onlyWifi: Boolean,
    networkType: NetworkType,
): Boolean = autoRefresh && onlyWifi && networkType != NetworkType.WIFI
