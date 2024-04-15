package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.preference.FeedAutoRefreshIntervalPreference
import com.ismartcoding.plain.preference.FeedAutoRefreshOnlyWifiPreference
import com.ismartcoding.plain.preference.FeedAutoRefreshPreference
import com.ismartcoding.plain.workers.FeedFetchWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FeedSettingsViewModel : ViewModel() {
    var showIntervalDialog = mutableStateOf(false)
    var autoRefresh = mutableStateOf(true)
    var autoRefreshInterval = mutableIntStateOf(7200)
    var autoRefreshOnlyWifi = mutableStateOf(false)

    fun loadSettings(context: Context) {
        viewModelScope.launch {
            autoRefresh.value = FeedAutoRefreshPreference.getAsync(context)
            autoRefreshInterval.value = FeedAutoRefreshIntervalPreference.getAsync(context)
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
}
