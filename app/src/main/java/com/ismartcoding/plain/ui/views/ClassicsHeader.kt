package com.ismartcoding.plain.ui.views

import android.content.Context
import android.view.LayoutInflater
import androidx.core.view.isVisible
import com.ismartcoding.lib.brv.PageRefreshLayout
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.R
import com.ismartcoding.plain.databinding.SrlClassicsHeaderBinding
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.scwang.smart.refresh.layout.api.RefreshHeader
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.constant.RefreshState
import java.util.*

class ClassicsHeader(context: Context, private val refreshLayout: RefreshLayout) : ClassicsAbstract(context, null, 0), RefreshHeader {
    private val binding = SrlClassicsHeaderBinding.inflate(LayoutInflater.from(context), this, true)

    private var KEY_LAST_UPDATE_TIME = "LAST_UPDATE_TIME"
    private var mLastTime: Date? = null
    private var mEnableLastTime = true
    var pullText: () -> String = { getString(R.string.srl_header_pulling) }
    var refreshingText: () -> String = { getString(R.string.srl_header_refreshing) }
    var releaseText: () -> String = { getString(R.string.srl_header_release) }
    var finishText: () -> String = { getString(R.string.srl_header_finish) }
    var failedText: () -> String = { getString(R.string.srl_header_failed) }

    override fun onFinish(
        refreshLayout: RefreshLayout,
        success: Boolean,
    ): Int {
        // if the dialog was destroyed, here may crash when the api request finished.
        try {
            if (success) {
                binding.title.text = finishText()
                if (mLastTime != null) {
                    setLastUpdateTime(Date())
                }
            } else {
                binding.title.text = failedText()
            }
        } catch (ex: Exception) {
            LogCat.e(ex.toString())
        }

        return super.onFinish(refreshLayout, success) // 延迟500毫秒之后再弹回
    }

    override fun onStateChanged(
        refreshLayout: RefreshLayout,
        oldState: RefreshState,
        newState: RefreshState,
    ) {
        // if the dialog was destroyed, here may crash when the api request finished.
        try {
            when (newState) {
                RefreshState.None -> {
                    binding.update.isVisible = mEnableLastTime
                    binding.title.text = pullText()
                }
                RefreshState.PullDownToRefresh -> {
                    binding.title.text = pullText()
                }
                RefreshState.Refreshing, RefreshState.RefreshReleased -> {
                    binding.title.text = refreshingText()
                }
                RefreshState.ReleaseToRefresh -> {
                    binding.title.text = releaseText()
                }
                RefreshState.ReleaseToTwoLevel -> {
                    binding.title.text = getString(R.string.srl_header_secondary)
                }
                RefreshState.Loading -> {
                    binding.update.visibility = if (mEnableLastTime) INVISIBLE else GONE
                    binding.title.text = getString(R.string.srl_header_loading)
                }
                else -> {}
            }
        } catch (ex: Exception) {
            LogCat.e(ex.toString())
        }
    }

    private fun setLastUpdateTime(time: Date): ClassicsHeader {
        mLastTime = time
        binding.update.text = LocaleHelper.getStringF(R.string.last_update, "time", time.formatDateTime())
        // MMKV.defaultMMKV().encode(KEY_LAST_UPDATE_TIME, time.time)
        return this
    }

    init {
        binding.update.isVisible = mEnableLastTime
        binding.title.text = pullText()
        KEY_LAST_UPDATE_TIME += (refreshLayout as PageRefreshLayout).pageName
        // setLastUpdateTime(Date(MMKV.defaultMMKV().decodeLong(KEY_LAST_UPDATE_TIME, System.currentTimeMillis())))
    }
}
