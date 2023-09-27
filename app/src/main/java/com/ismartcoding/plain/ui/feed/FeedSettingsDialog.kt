package com.ismartcoding.plain.ui.feed

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.preference.FeedAutoRefreshIntervalPreference
import com.ismartcoding.plain.data.preference.FeedAutoRefreshOnlyWifiPreference
import com.ismartcoding.plain.data.preference.FeedAutoRefreshPreference
import com.ismartcoding.plain.databinding.DialogFeedSettingsBinding
import com.ismartcoding.plain.features.feed.FeedAutoRefreshInterval
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.FormatHelper
import com.ismartcoding.plain.ui.BaseBottomSheetDialog
import com.ismartcoding.plain.ui.SelectOptionsDialog
import com.ismartcoding.plain.ui.extensions.*
import com.ismartcoding.plain.workers.FeedFetchWorker
import kotlinx.coroutines.launch

class FeedSettingsDialog : BaseBottomSheetDialog<DialogFeedSettingsBinding>() {
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            val context = requireContext()
            binding.autoRefresh.setKeyText(R.string.auto_refresh_feeds)
                .setSwitch(withIO { FeedAutoRefreshPreference.getAsync(context) }, onChanged = { _, isEnabled ->
                    lifecycleScope.launch {
                        withIO { FeedAutoRefreshPreference.putAsync(context, isEnabled) }
                        if (isEnabled) {
                            FeedFetchWorker.startRepeatWorkerAsync(context)
                        } else {
                            FeedFetchWorker.cancelRepeatWorker()
                        }
                    }
                })
            binding.autoRefreshInterval.setKeyText(R.string.auto_refresh_interval)
                .setValueText(FormatHelper.formatSeconds(withIO { FeedAutoRefreshIntervalPreference.getAsync(context) }))
                .showMore()
                .setClick {
                    val options = setOf(900, 1800, 3600, 7200, 21600, 43200, 86400).map { FeedAutoRefreshInterval(it) }
                    SelectOptionsDialog(LocaleHelper.getString(R.string.auto_refresh_interval), options) {
                        lifecycleScope.launch {
                            FeedAutoRefreshIntervalPreference.putAsync(context, it.value)
                            binding.autoRefreshInterval.setValueText(
                                FormatHelper.formatSeconds(withIO { FeedAutoRefreshIntervalPreference.getAsync(context) }),
                            )
                        }
                    }.show()
                }

            binding.autoRefreshOnlyWifi.setKeyText(R.string.auto_refresh_only_over_wifi)
                .setSwitch(withIO { FeedAutoRefreshOnlyWifiPreference.getAsync(context) }, onChanged = { _, isEnabled ->
                    lifecycleScope.launch {
                        withIO {
                            FeedAutoRefreshOnlyWifiPreference.putAsync(context, isEnabled)
                        }
                    }
                })
        }
    }
}
