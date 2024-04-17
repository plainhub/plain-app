package com.ismartcoding.plain.ui.wireguard

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.brv.utils.linear
import com.ismartcoding.lib.brv.utils.setup
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.DeleteWireGuardMutation
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.api.BoxApi
import com.ismartcoding.plain.data.UIDataCache
import com.ismartcoding.plain.databinding.DialogWireguardsBinding
import com.ismartcoding.plain.databinding.ViewListItemBinding
import com.ismartcoding.plain.features.box.ApplyWireGuardResultEvent
import com.ismartcoding.plain.features.box.FetchWireGuardsEvent
import com.ismartcoding.plain.features.box.WireGuardsResultEvent
import com.ismartcoding.plain.features.wireguard.WireGuard
import com.ismartcoding.plain.features.wireguard.bindWireGuard
import com.ismartcoding.plain.ui.BaseDialog
import com.ismartcoding.plain.ui.extensions.enableSwipeMenu
import com.ismartcoding.plain.ui.extensions.onBack
import com.ismartcoding.plain.ui.extensions.setRightSwipeButton
import com.ismartcoding.plain.ui.extensions.setSafeClick
import com.ismartcoding.plain.ui.extensions.setScrollBehavior
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.launch

class WireGuardsDialog : BaseDialog<DialogWireguardsBinding>() {
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.topAppBar.apply {
            toolbar.setTitle(R.string.wireguard)
            toolbar.onBack {
                dismiss()
            }
            setScrollBehavior(false)
        }
        binding.list.rv.linear().setup {
            addType<WireGuard>(R.layout.view_list_item)
            onBind {
                val b = getBinding<ViewListItemBinding>()
                val m = getModel<WireGuard>()
                b.bindWireGuard(lifecycleScope, m)
                b.enableSwipeMenu(true)
                b.setRightSwipeButton(getString(R.string.delete)) {
                    DialogHelper.confirmToAction(R.string.confirm_to_delete) {
                        lifecycleScope.launch {
                            DialogHelper.showLoading()
                            val r = withIO { BoxApi.mixMutateAsync(DeleteWireGuardMutation(m.id)) }
                            DialogHelper.hideLoading()
                            if (!r.isSuccess()) {
                                DialogHelper.showErrorDialog(r.getErrorMessage())
                                return@launch
                            }

                            UIDataCache.current().wireGuards?.removeAll { it.id == m.id }
                            updateUI()
                        }
                    }
                }
            }
        }

        binding.list.page.onRefresh {
            fetch()
        }

        binding.fab.setSafeClick {
            val wg = WireGuard.createNew()
            wg.id = WireGuard.generateId(UIDataCache.current().wireGuards ?: arrayListOf())
            WireGuardConfigDialog(wg).show()
        }

        receiveEvent<ApplyWireGuardResultEvent> {
            updateUI()
        }

        receiveEvent<WireGuardsResultEvent> { event ->
            val r = event.result
            if (!r.isSuccess()) {
                DialogHelper.showMessage(r)
                binding.list.page.finishRefresh(false)
                binding.list.page.showError()
                return@receiveEvent
            }
            updateUI()
        }

        if (UIDataCache.current().wireGuards == null) {
            binding.list.page.showLoading()
        } else {
            updateUI()
        }
    }

    private fun updateUI() {
        binding.list.page.replaceData(UIDataCache.current().wireGuards ?: arrayListOf())
    }

    private fun fetch() {
        sendEvent(FetchWireGuardsEvent(TempData.selectedBoxId))
    }
}
