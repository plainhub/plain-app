package com.ismartcoding.plain.ui.hostapd

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.softinput.setWindowSoftInput
import com.ismartcoding.plain.ApplyHostapdMutation
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.api.BoxApi
import com.ismartcoding.plain.data.UIDataCache
import com.ismartcoding.plain.databinding.DialogHostapdBinding
import com.ismartcoding.plain.features.box.ApplyHostapdResultEvent
import com.ismartcoding.plain.features.box.FetchNetworkConfigEvent
import com.ismartcoding.plain.features.box.NetworkConfigResultEvent
import com.ismartcoding.plain.features.hostapd.HostapdConfig
import com.ismartcoding.plain.ui.BaseBottomSheetDialog
import com.ismartcoding.plain.ui.extensions.initMenu
import com.ismartcoding.plain.ui.extensions.onMenuItemClick
import com.ismartcoding.plain.ui.extensions.setSafeClick
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.views.LoadingButtonView
import kotlinx.coroutines.launch

class HostapdDialog : BaseBottomSheetDialog<DialogHostapdBinding>() {
    private val mConfig = HostapdConfig()
    private var mEnabled = false

    override fun getSubmitButton(): LoadingButtonView {
        return binding.button
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.topAppBar.run {
            initMenu(R.menu.more)
            onMenuItemClick {
                when (itemId) {
                    R.id.more -> {
                        HostapdConfigDialog().show()
                    }
                }
            }
        }

        receiveEvent<NetworkConfigResultEvent> { event ->
            binding.page.showContent()
            val r = event.result
            if (!r.isSuccess()) {
                DialogHelper.showMessage(r)
                binding.page.finishRefresh(false)
                return@receiveEvent
            }

            updateUI()
            binding.page.finishRefresh(r.isRealSuccess())
        }

        receiveEvent<ApplyHostapdResultEvent> { event ->
            val r = event.result
            if (!r.isSuccess()) {
                binding.page.finishRefresh(false)
                return@receiveEvent
            }

            updateUI()
            binding.page.finishRefresh(r.isRealSuccess())
        }

        binding.button.enable(false)
        binding.page.onRefresh {
            fetch()
        }
        binding.page.showLoading()

        addFormItem(binding.ssid)
        addFormItem(binding.password)

        setWindowSoftInput(binding.button)
    }

    private fun updateUI() {
        binding.button.enable(true)

        val current = UIDataCache.current()
        current.hostapd?.let {
            mConfig.load(it.config)
        }

        val hostapd = current.hostapd
        mEnabled = hostapd?.isEnabled == true

        binding.enable.setSwitch(mEnabled) { _, isEnabled ->
            mEnabled = isEnabled
        }

        binding.ssid.run {
            text = mConfig.ssid
            onValidate = {
                mConfig.validateSSID(it)
            }
            onTextChanged = {
                mConfig.ssid = text.trim()
            }
        }
        binding.password.run {
            text = mConfig.password
            onValidate = {
                mConfig.validatePassword(it)
            }
            onTextChanged = {
                mConfig.password = text.trim()
            }
        }
        binding.hideSsid.setSwitch(mConfig.hideSsid) { _, isEnabled ->
            mConfig.hideSsid = isEnabled
        }

        binding.button.setSafeClick {
            if (hasInputError()) {
                return@setSafeClick
            }

            lifecycleScope.launch {
                blockFormUI()
                val r =
                    withIO {
                        BoxApi.mixMutateAsync(ApplyHostapdMutation(mConfig.toConfig(), mEnabled))
                    }
                unblockFormUI()
                if (!r.isSuccess()) {
                    DialogHelper.showErrorDialog(r.getErrorMessage())
                    return@launch
                }

                r.response?.data?.applyHostapd?.hostapdFragment?.let {
                    UIDataCache.current().hostapd = it
                }
                dismiss()
            }
        }
    }

    private fun fetch() {
        sendEvent(FetchNetworkConfigEvent(TempData.selectedBoxId))
    }
}
