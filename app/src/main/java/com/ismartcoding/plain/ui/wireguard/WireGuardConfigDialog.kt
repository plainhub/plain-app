package com.ismartcoding.plain.ui.wireguard

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.softinput.setWindowSoftInput
import com.ismartcoding.plain.ApplyWireGuardMutation
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.api.BoxApi
import com.ismartcoding.plain.data.UIDataCache
import com.ismartcoding.plain.databinding.DialogWireguardConfigBinding
import com.ismartcoding.plain.features.box.ApplyWireGuardResultEvent
import com.ismartcoding.plain.features.box.FetchWireGuardsEvent
import com.ismartcoding.plain.features.box.WireGuardsResultEvent
import com.ismartcoding.plain.features.wireguard.WireGuard
import com.ismartcoding.plain.features.wireguard.toWireGuard
import com.ismartcoding.plain.ui.BaseDialog
import com.ismartcoding.plain.ui.extensions.initMenu
import com.ismartcoding.plain.ui.extensions.onBack
import com.ismartcoding.plain.ui.extensions.onMenuItemClick
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.launch

class WireGuardConfigDialog(val wireGuard: WireGuard) : BaseDialog<DialogWireguardConfigBinding>() {
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
            title = "${wireGuard.id}.conf"
            initMenu(R.menu.wireguard_config, overflow = true)

            onBack {
                onBackPressed()
            }

            onMenuItemClick {
                when (itemId) {
                    R.id.save -> {
                        doSave()
                    }

                    R.id.add_peer -> {
                        lifecycleScope.launch {
                            val wg = WireGuard()
                            wg.parse(binding.editor.getText())
                            binding.editor.initViewAsync(
                                lifecycle,
                                "${wg.raw}\n\n" + wg.generateNewPeer().toString(),
                                "ini",
                            )
                        }
                    }
                }
            }
        }

        binding.page.onRefresh {
            fetch()
        }

        receiveEvent<WireGuardsResultEvent> { event ->
            val r = event.result
            if (!r.isSuccess()) {
                DialogHelper.showMessage(r)
                binding.page.finishRefresh(false)
                return@receiveEvent
            }

            UIDataCache.current().wireGuards?.let { wgs ->
                val wg = wgs.find { it.id == wireGuard.id }
                if (wg != null) {
                    wireGuard.raw = wg.raw
                }
            }
            updateUI()
            binding.page.finishRefresh(r.isRealSuccess())
        }

        setWindowSoftInput(binding.editor)
        updateUI()
    }

    private fun updateUI() {
        lifecycleScope.launch {
            binding.editor.initViewAsync(lifecycle, wireGuard.raw, "ini")
        }
    }

    private fun fetch() {
        sendEvent(FetchWireGuardsEvent(TempData.selectedBoxId))
    }

    private fun doSave() {
        lifecycleScope.launch {
            DialogHelper.showLoading()
            val wg = WireGuard()
            wg.parse(binding.editor.getText())
            val content = wg.toString()
            val r =
                withIO {
                    BoxApi.mixMutateAsync(
                        ApplyWireGuardMutation(
                            wireGuard.id,
                            content,
                            wireGuard.isEnabled,
                        ),
                    )
                }
            DialogHelper.hideLoading()
            if (!r.isSuccess()) {
                DialogHelper.showErrorDialog(r.getErrorMessage())
                return@launch
            }

            r.response?.data?.applyWireGuard?.wireGuardFragment?.let { wg2 ->
                UIDataCache.current().wireGuards?.run {
                    val index = indexOfFirst { it.id == wireGuard.id }
                    if (index != -1) {
                        removeAt(index)
                        add(index, wg2.toWireGuard())
                    } else {
                        add(wg2.toWireGuard())
                    }
                }
            }
            sendEvent(ApplyWireGuardResultEvent(TempData.selectedBoxId, r))
            dismiss()
        }
    }
}
