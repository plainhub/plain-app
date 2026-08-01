package com.ismartcoding.plain.platform

import com.ismartcoding.plain.features.feed.FeedFetcher
import com.ismartcoding.plain.features.feed.FeedWorkerState
import com.ismartcoding.plain.preferences.FeedAutoRefreshIntervalPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private val feedWorkerScope = CoroutineScope(IODispatcher)
private var feedRepeatJob: Job? = null

actual fun feedWorkerOneTimeRequest(feedId: String) {
    feedWorkerScope.launch { FeedFetcher.fetchOne(feedId) }
}

actual fun feedWorkerStartRepeat() {
    feedRepeatJob?.cancel()
    feedRepeatJob = feedWorkerScope.launch {
        while (isActive) {
            FeedFetcher.fetchAll(autoRefresh = true)
            val intervalSec = FeedAutoRefreshIntervalPreference.getAsync().toLong()
            delay(intervalSec * 1000L)
        }
    }
}

actual fun feedWorkerCancelRepeat() {
    feedRepeatJob?.cancel()
    feedRepeatJob = null
    FeedWorkerState.clearAll()
}
