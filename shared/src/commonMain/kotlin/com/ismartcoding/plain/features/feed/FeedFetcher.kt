package com.ismartcoding.plain.features.feed

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.platform.NetworkType
import com.ismartcoding.plain.platform.getNetworkType
import com.ismartcoding.plain.preferences.UserPrefs

object FeedFetcher {
    suspend fun fetchOne(feedId: String) = RustContentApi.sync(feedId)
    suspend fun fetchAll(autoRefresh: Boolean = false) {
        if (shouldSkipAutoRefresh(autoRefresh, UserPrefs.feedAutoRefreshOnlyWifi.value, getNetworkType())) return
        RustContentApi.sync("")
    }
}
internal fun shouldSkipAutoRefresh(autoRefresh: Boolean, onlyWifi: Boolean, networkType: NetworkType): Boolean = autoRefresh && onlyWifi && networkType != NetworkType.WIFI
