package com.ismartcoding.plain.ui.feeds

import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.ui.state.launchSafe

import com.ismartcoding.plain.preferences.*

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.lib.TimeHelper
import kotlinx.coroutines.launch
import kotlin.time.Instant

class FeedSettingsViewModel internal constructor(val source: ContentSource = ContentSource.Local) : ViewModel() {
    var showIntervalDialog = mutableStateOf(false)
    var autoRefresh = mutableStateOf(true)
    var autoRefreshInterval = mutableIntStateOf(7200)
    var autoRefreshOnlyWifi = mutableStateOf(false)
    var showClearFeedsDialog = mutableStateOf(false)
    var clearFeedItemsTs = mutableLongStateOf(Constants.ONE_DAY * 7)

    init {
        if (source is ContentSource.Remote) {
            viewModelScope.launchSafe { loadRemoteSettings() }
        } else {
            viewModelScope.launch { Prefs.feedAutoRefresh.collect { autoRefresh.value = it } }
            viewModelScope.launch { Prefs.feedAutoRefreshInterval.collect { autoRefreshInterval.intValue = it } }
            viewModelScope.launch { Prefs.feedAutoRefreshOnlyWifi.collect { autoRefreshOnlyWifi.value = it } }
        }
    }

    fun setAutoRefresh(value: Boolean) {
        viewModelScope.launchSafe {
            if (source is ContentSource.Remote) {
                saveRemoteSetting(PrefKeys.FEED_AUTO_REFRESH, value)
                autoRefresh.value = value
            } else Prefs.set(PrefKeys.FEED_AUTO_REFRESH, value)
        }
    }

    fun setAutoRefreshInterval(value: Int) {
        viewModelScope.launchSafe {
            if (source is ContentSource.Remote) {
                saveRemoteSetting(PrefKeys.FEED_AUTO_REFRESH_INTERVAL, value)
                autoRefreshInterval.intValue = value
            } else Prefs.set(PrefKeys.FEED_AUTO_REFRESH_INTERVAL, value)
        }
    }

    fun setAutoRefreshOnlyWifi(value: Boolean) {
        viewModelScope.launchSafe {
            if (source is ContentSource.Remote) {
                saveRemoteSetting(PrefKeys.FEED_AUTO_REFRESH_ONLY_WIFI, value)
                autoRefreshOnlyWifi.value = value
            } else Prefs.set(PrefKeys.FEED_AUTO_REFRESH_ONLY_WIFI, value)
        }
    }

    private suspend fun loadRemoteSettings() {
        val values = source.api.execute(com.ismartcoding.plain.graphql.Query.prefs())
        autoRefresh.value = values[PrefKeys.FEED_AUTO_REFRESH]?.let { com.ismartcoding.plain.lib.JsonHelper.jsonDecodeFromElement<Boolean>(it) } ?: true
        autoRefreshInterval.intValue = values[PrefKeys.FEED_AUTO_REFRESH_INTERVAL]?.let { com.ismartcoding.plain.lib.JsonHelper.jsonDecodeFromElement<Int>(it) } ?: 7200
        autoRefreshOnlyWifi.value = values[PrefKeys.FEED_AUTO_REFRESH_ONLY_WIFI]?.let { com.ismartcoding.plain.lib.JsonHelper.jsonDecodeFromElement<Boolean>(it) } ?: false
    }

    private suspend inline fun <reified T> saveRemoteSetting(key: String, value: T) {
        check(source.api.execute(com.ismartcoding.plain.graphql.Mutation.setPref(key, com.ismartcoding.plain.lib.JsonHelper.jsonEncodeToElement(value)))) { "preference_update_failed" }
    }

    suspend fun clearByFeedIdAsync(feedId: String) = withIO {
        source.feedEntries.deleteQuery("feed_id:$feedId")
    }

    fun clearAllAsync() {
        viewModelScope.launchSafe {
            source.feedEntries.deleteAllAsync()
        }
    }

    suspend fun clearByTimeAsync(ts: Long) = withIO {
        val time = TimeHelper.now().epochSeconds - ts
        source.feedEntries.deleteQuery("created_at:<${Instant.fromEpochSeconds(time)}")
    }
}
