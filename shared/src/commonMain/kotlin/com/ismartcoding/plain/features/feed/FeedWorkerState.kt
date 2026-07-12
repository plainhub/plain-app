package com.ismartcoding.plain.features.feed

object FeedWorkerState {
    val statusMap = mutableMapOf<String, FeedWorkerStatus>()
    val errorMap = mutableMapOf<String, String>()

    fun clear(feedId: String) {
        statusMap.remove(feedId)
        errorMap.remove(feedId)
    }

    fun clearAll() {
        statusMap.clear()
        errorMap.clear()
    }
}
