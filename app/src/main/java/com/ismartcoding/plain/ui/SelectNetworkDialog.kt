package com.ismartcoding.plain.ui

import android.os.Bundle
import android.view.View
import com.ismartcoding.lib.brv.utils.linear
import com.ismartcoding.lib.brv.utils.setup
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.data.AllItemsOption
import com.ismartcoding.plain.data.UIDataCache
import com.ismartcoding.plain.databinding.DialogSelectItemBinding
import com.ismartcoding.plain.databinding.ViewListItemBinding
import com.ismartcoding.plain.features.box.FetchNetworksEvent
import com.ismartcoding.plain.features.box.NetworksResultEvent
import com.ismartcoding.plain.features.network.bindNetwork
import com.ismartcoding.plain.fragment.NetworkFragment
import com.ismartcoding.plain.ui.extensions.clearTextRows
import com.ismartcoding.plain.ui.extensions.setClick
import com.ismartcoding.plain.ui.extensions.setKeyText
import com.ismartcoding.plain.ui.helpers.DialogHelper

class SelectNetworkDialog(val onSelect: (String) -> Unit) : BaseBottomSheetDialog<DialogSelectItemBinding>() {
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.list.rv.isNestedScrollingEnabled = false
        binding.list.rv.linear().setup {
            addType<AllItemsOption>(R.layout.view_list_item)
            addType<NetworkFragment>(R.layout.view_list_item)
            onBind {
                val binding = getBinding<ViewListItemBinding>()
                val m = getModel<Any>()
                var value = ""
                when (m) {
                    is AllItemsOption -> {
                        value = ""
                        binding.setKeyText(m.name)
                        binding.clearTextRows()
                    }
                    is NetworkFragment -> {
                        value = m.ifName
                        binding.bindNetwork(requireContext(), m)
                    }
                }
                binding.setClick {
                    onSelect(value)
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
        val items = mutableListOf<Any>()
        items.add(AllItemsOption(getString(R.string.all_local_networks)))
        items.addAll(UIDataCache.current().getSelectableNetworks().sortedBy { it.name })
        binding.list.page.replaceData(items)
    }

    private fun fetch() {
        sendEvent(FetchNetworksEvent(TempData.selectedBoxId))
    }
}
