package com.ismartcoding.plain.ui.models

import com.ismartcoding.plain.preferences.*

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.features.feed.FeedEntryHelper
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.platform.feedWorkerCancelRepeat
import com.ismartcoding.plain.platform.feedWorkerStartRepeat
import kotlinx.coroutines.launch
import kotlin.time.Instant

class FeedSettingsViewModel : ViewModel() {
    var showIntervalDialog = mutableStateOf(false)
    var autoRefresh = mutableStateOf(true)
    var autoRefreshInterval = mutableIntStateOf(7200)
    var autoRefreshOnlyWifi = mutableStateOf(false)
    var showClearFeedsDialog = mutableStateOf(false)
    var clearFeedItemsTs = mutableLongStateOf(Constants.ONE_DAY * 7)

    fun loadSettings() {
        viewModelScope.launch {
            autoRefresh.value = UserPrefs.feedAutoRefresh.value
            autoRefreshInterval.intValue = UserPrefs.feedAutoRefreshInterval.value
            autoRefreshOnlyWifi.value = UserPrefs.feedAutoRefreshOnlyWifi.value
        }
    }

    fun setAutoRefresh(value: Boolean) {
        autoRefresh.value = value
        viewModelScope.launchSafe {
            UserPrefs.feedAutoRefresh.value = value
            if (value) {
                feedWorkerStartRepeat()
            } else {
                feedWorkerCancelRepeat()
            }
        }
    }

    fun setAutoRefreshInterval(value: Int) {
        autoRefreshInterval.value = value
        viewModelScope.launchSafe {
            UserPrefs.feedAutoRefreshInterval.value = value
        }
    }

    fun setAutoRefreshOnlyWifi(value: Boolean) {
        autoRefreshOnlyWifi.value = value
        viewModelScope.launchSafe {
            UserPrefs.feedAutoRefreshOnlyWifi.value = value
        }
    }

    suspend fun clearByFeedIdAsync(feedId: String) = withIO {
        val ids = FeedEntryHelper.getIdsAsync("feed_id:$feedId")
        TagHelper.deleteTagRelationByKeys(ids, DataType.FEED_ENTRY)
        FeedEntryHelper.deleteAsync(ids)
    }

    fun clearAllAsync() {
        viewModelScope.launchSafe {
            TagHelper.deleteByTypeAsync(DataType.FEED_ENTRY)
            FeedEntryHelper.deleteAllAsync()
        }
    }

    suspend fun clearByTimeAsync(ts: Long) = withIO {
        val time = TimeHelper.now().epochSeconds - ts
        val ids = FeedEntryHelper.getIdsAsync("created_at:<${Instant.fromEpochSeconds(time)}")
        TagHelper.deleteTagRelationByKeys(ids, DataType.FEED_ENTRY)
        FeedEntryHelper.deleteAsync(ids)
    }
}
