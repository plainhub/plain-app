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
import com.ismartcoding.plain.databinding.DialogHostapdConfigBinding
import com.ismartcoding.plain.features.box.ApplyHostapdResultEvent
import com.ismartcoding.plain.features.box.FetchNetworkConfigEvent
import com.ismartcoding.plain.features.box.NetworkConfigResultEvent
import com.ismartcoding.plain.features.hostapd.HostapdConfig
import com.ismartcoding.plain.ui.BaseDialog
import com.ismartcoding.plain.ui.extensions.initMenu
import com.ismartcoding.plain.ui.extensions.onBack
import com.ismartcoding.plain.ui.extensions.onMenuItemClick
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.launch

class HostapdConfigDialog : BaseDialog<DialogHostapdConfigBinding>() {
    override fun onBackPressed() {
        if (binding.editor.isChanged()) {
            DialogHelper.confirmToLeave {
                dismiss()
            }
        } else {
            dismiss()
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        binding.topAppBar.toolbar.run {
            title = "hostapd.conf"
            initMenu(R.menu.hostapd_config)
            onBack {
                onBackPressed()
            }
            onMenuItemClick {
                when (itemId) {
                    R.id.save -> {
                        val hostapdConfig = HostapdConfig()
                        hostapdConfig.load(binding.editor.getText())
                        val validateResult = hostapdConfig.validate()
                        if (validateResult.isNotEmpty()) {
                            DialogHelper.showErrorDialog(validateResult)
                            return@onMenuItemClick
                        }

                        lifecycleScope.launch {
                            DialogHelper.showLoading()
                            val current = UIDataCache.current()
                            val hostapd = current.hostapd
                            val r =
                                withIO {
                                    BoxApi.mixMutateAsync(ApplyHostapdMutation(binding.editor.getText(), hostapd?.isEnabled == true))
                                }
                            DialogHelper.hideLoading()
                            if (!r.isSuccess()) {
                                DialogHelper.showErrorDialog(r.getErrorMessage())
                                return@launch
                            }

                            r.response?.data?.applyHostapd?.hostapdFragment?.let {
                                UIDataCache.current().hostapd = it
                            }
                            sendEvent(ApplyHostapdResultEvent(TempData.selectedBoxId, r))
                            dismiss()
                        }
                    }
                }
            }
        }

        binding.page.onRefresh {
            fetch()
        }

        receiveEvent<NetworkConfigResultEvent> { event ->
            val r = event.result
            binding.page.showContent()
            if (!r.isSuccess()) {
                DialogHelper.showMessage(r)
                binding.page.finishRefresh(false)
                return@receiveEvent
            }

            updateUI()
            binding.page.finishRefresh(r.isRealSuccess())
        }
        setWindowSoftInput(binding.editor)

        updateUI()

        binding.page.showLoading()
    }

    private fun updateUI() {
        lifecycleScope.launch {
            UIDataCache.current().hostapd?.let {
                binding.editor.initViewAsync(lifecycle, it.config, "ini")
            }
        }
    }

    private fun fetch() {
        sendEvent(FetchNetworkConfigEvent(TempData.selectedBoxId))
    }
}
