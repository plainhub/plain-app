package com.ismartcoding.plain.ui.network

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayout
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.softinput.setWindowSoftInput
import com.ismartcoding.plain.ApplyNetplanAndNetmixMutation
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.api.BoxApi
import com.ismartcoding.plain.api.HttpApiTimeout
import com.ismartcoding.plain.data.UIDataCache
import com.ismartcoding.plain.databinding.DialogNetworkConfigBinding
import com.ismartcoding.plain.features.box.FetchNetworkConfigEvent
import com.ismartcoding.plain.features.box.NetworkConfigResultEvent
import com.ismartcoding.plain.ui.BaseDialog
import com.ismartcoding.plain.ui.extensions.initMenu
import com.ismartcoding.plain.ui.extensions.onBack
import com.ismartcoding.plain.ui.extensions.onMenuItemClick
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.launch

class NetworkConfigDialog : BaseDialog<DialogNetworkConfigBinding>() {
    override fun onBackPressed() {
        if (binding.netplan.isChanged() || binding.netmix.isChanged()) {
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
            setTitle(R.string.network_config)

            initMenu(R.menu.network_config)
            onBack {
                onBackPressed()
            }

            onMenuItemClick {
                when (itemId) {
                    R.id.save -> {
                        val netplan = binding.netplan.getText()
                        val netmix = binding.netmix.getText()
                        if (netplan.isEmpty() || netmix.isEmpty()) {
                            DialogHelper.showErrorDialog( "Network config should not be empty.")
                            return@onMenuItemClick
                        }

                        lifecycleScope.launch {
                            DialogHelper.showLoading()
                            val r =
                                withIO {
                                    BoxApi.mixMutateAsync(
                                        ApplyNetplanAndNetmixMutation(netplan, netmix),
                                        timeout = HttpApiTimeout.MEDIUM_SECONDS,
                                    )
                                }
                            DialogHelper.hideLoading()
                            if (!r.isSuccess()) {
                                DialogHelper.showErrorDialog( r.getErrorMessage())
                                return@launch
                            }

                            r.response?.data?.applyNetmix?.networkConfigFragment?.let {
                                UIDataCache.current().networkConfig = it
                            }
                            dismiss()
                        }
                    }
                }
            }
        }

        binding.tabs.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    if (tab.position == 0) {
                        binding.netplan.visibility = View.VISIBLE
                    } else {
                        binding.netmix.visibility = View.VISIBLE
                    }
                    binding.page.setEnableRefresh(true)
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {
                    if (tab.position == 0) {
                        binding.netplan.visibility = View.GONE
                    } else {
                        binding.netmix.visibility = View.GONE
                    }
                }

                override fun onTabReselected(tab: TabLayout.Tab) {
                }
            },
        )

        binding.page.onRefresh {
            fetch()
        }
        binding.netplan.setRefreshStateListener {
            binding.page.setEnableRefresh(it)
        }
        binding.netmix.setRefreshStateListener {
            binding.page.setEnableRefresh(it)
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
        setWindowSoftInput(binding.page)
        updateUI()
        binding.page.showLoading()
    }

    private fun updateUI() {
        lifecycleScope.launch {
            UIDataCache.current().networkConfig?.let {
                binding.netplan.initViewAsync(lifecycle, it.netplan, "yaml")
                binding.netmix.initViewAsync(lifecycle, it.netmix, "yaml")
            }
        }
    }

    private fun fetch() {
        sendEvent(FetchNetworkConfigEvent(TempData.selectedBoxId))
    }
}
