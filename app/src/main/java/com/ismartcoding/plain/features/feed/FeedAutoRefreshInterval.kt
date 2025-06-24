package com.ismartcoding.plain.features.feed

import android.content.Context
import com.ismartcoding.plain.data.ISelectOption
import com.ismartcoding.plain.preferences.FeedAutoRefreshIntervalPreference
import com.ismartcoding.plain.helpers.FormatHelper

class FeedAutoRefreshInterval(val value: Int) : ISelectOption {
    override suspend fun isSelected(context: Context): Boolean {
        return value == FeedAutoRefreshIntervalPreference.getAsync(context)
    }

    override fun getText(): String {
        return FormatHelper.formatSeconds(value)
    }
}
