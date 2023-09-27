package com.ismartcoding.plain.ui

import android.os.Bundle
import android.view.View
import com.ismartcoding.lib.brv.utils.linear
import com.ismartcoding.lib.brv.utils.setup
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.data.UIDataCache
import com.ismartcoding.plain.databinding.DialogSelectItemBinding
import com.ismartcoding.plain.databinding.ViewListItemBinding
import com.ismartcoding.plain.features.box.FetchNetworksEvent
import com.ismartcoding.plain.features.box.NetworksResultEvent
import com.ismartcoding.plain.features.network.bindNetwork
import com.ismartcoding.plain.fragment.NetworkFragment
import com.ismartcoding.plain.ui.extensions.setClick
import com.ismartcoding.plain.ui.helpers.DialogHelper

class SelectInterfaceDialog(val networks: List<NetworkFragment>, val onSelect: (String) -> Unit) : BaseBottomSheetDialog<DialogSelectItemBinding>() {
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        binding.list.rv.isNestedScrollingEnabled = false
        binding.list.rv.linear().setup {
            addType<NetworkFragment>(R.layout.view_list_item)
            onBind {
                val binding = getBinding<ViewListItemBinding>()
                val m = getModel<NetworkFragment>()
                binding.bindNetwork(requireContext(), m)
                binding.setClick {
                    onSelect(m.ifName)
                    dismiss()
                }
            }
        }

        binding.list.page.onRefresh {
            fetch()
        }

        receiveEvent<NetworksResultEvent> { event ->
            val r = event.result
            if (!r.isSuccess()) {
                DialogHelper.showMessage(r)
                binding.list.page.finishRefresh(false)
                binding.list.page.showError()
                return@receiveEvent
            }
            search()
        }

        if (UIDataCache.current().networks == null) {
            binding.list.page.showLoading()
        } else {
            search()
        }
    }

    private fun search() {
        binding.list.page.replaceData(networks)
    }

    private fun fetch() {
        sendEvent(FetchNetworksEvent(TempData.selectedBoxId))
    }
}
