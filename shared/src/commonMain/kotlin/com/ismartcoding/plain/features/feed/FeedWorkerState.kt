package com.ismartcoding.plain.features.feed

import androidx.compose.runtime.mutableStateMapOf

object FeedWorkerState {
    // Mutated concurrently by parallel feed sync tasks (FeedFetcher.pmap()), so
    // a plain mutableMapOf (LinkedHashMap) is not safe here.
    val statusMap = mutableStateMapOf<String, FeedWorkerStatus>()
    val errorMap = mutableStateMapOf<String, String>()

    fun clear(feedId: String) {
        statusMap.remove(feedId)
        errorMap.remove(feedId)
    }

    fun clearAll() {
        statusMap.clear()
        errorMap.clear()
    }
}
