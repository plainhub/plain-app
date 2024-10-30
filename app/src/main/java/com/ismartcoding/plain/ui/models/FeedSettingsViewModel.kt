package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.features.feed.FeedEntryHelper
import com.ismartcoding.plain.preference.FeedAutoRefreshIntervalPreference
import com.ismartcoding.plain.preference.FeedAutoRefreshOnlyWifiPreference
import com.ismartcoding.plain.preference.FeedAutoRefreshPreference
import com.ismartcoding.plain.workers.FeedFetchWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class FeedSettingsViewModel : ViewModel() {
    var showIntervalDialog = mutableStateOf(false)
    var autoRefresh = mutableStateOf(true)
    var autoRefreshInterval = mutableIntStateOf(7200)
    var autoRefreshOnlyWifi = mutableStateOf(false)
    var showClearFeedsDialog = mutableStateOf(false)
    var clearFeedItemsTs = mutableLongStateOf(Constants.ONE_DAY * 7)

    fun loadSettings(context: Context) {
        viewModelScope.launch {
            autoRefresh.value = FeedAutoRefreshPreference.getAsync(context)
            autoRefreshInterval.intValue = FeedAutoRefreshIntervalPreference.getAsync(context)
            autoRefreshOnlyWifi.value = FeedAutoRefreshOnlyWifiPreference.getAsync(context)
        }
    }

    fun setAutoRefresh(context: Context, value: Boolean) {
        autoRefresh.value = value
        viewModelScope.launch(Dispatchers.IO) {
            FeedAutoRefreshPreference.putAsync(context, value)
            if (value) {
                FeedFetchWorker.startRepeatWorkerAsync(context)
            } else {
                FeedFetchWorker.cancelRepeatWorker()
            }
        }
    }

    fun setAutoRefreshInterval(context: Context, value: Int) {
        autoRefreshInterval.value = value
        viewModelScope.launch(Dispatchers.IO) {
            FeedAutoRefreshIntervalPreference.putAsync(context, value)
        }
    }

    fun setAutoRefreshOnlyWifi(context: Context, value: Boolean) {
        autoRefreshOnlyWifi.value = value
        viewModelScope.launch(Dispatchers.IO) {
            FeedAutoRefreshOnlyWifiPreference.putAsync(context, value)
        }
    }

    suspend fun clearByFeedIdAsync(feedId: String) {
        val ids = FeedEntryHelper.getIdsAsync("feed_id:$feedId")
        TagHelper.deleteTagRelationByKeys(ids, DataType.FEED_ENTRY)
        FeedEntryHelper.deleteAsync(ids)
    }

    fun clearAllAsync() {
        TagHelper.deleteByTypeAsync(DataType.FEED_ENTRY)
        FeedEntryHelper.deleteAllAsync()
    }

    suspend fun clearByTimeAsync(ts: Long) {
        val time = Clock.System.now().epochSeconds - ts
        val ids = FeedEntryHelper.getIdsAsync("created_at:<${Instant.fromEpochSeconds(time)}")
        TagHelper.deleteTagRelationByKeys(ids, DataType.FEED_ENTRY)
        FeedEntryHelper.deleteAsync(ids)
    }
}
