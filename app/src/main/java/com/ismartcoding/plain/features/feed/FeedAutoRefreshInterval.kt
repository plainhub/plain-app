package com.ismartcoding.plain.features.feed
import com.ismartcoding.plain.preferences.*

import android.content.Context
import com.ismartcoding.plain.data.ISelectOption
import com.ismartcoding.plain.preferences.FeedAutoRefreshIntervalPreference

class FeedAutoRefreshInterval(val value: Int) : ISelectOption {
    override suspend fun isSelected(context: Context): Boolean {
        return value == FeedAutoRefreshIntervalPreference.getAsync()
    }
}
