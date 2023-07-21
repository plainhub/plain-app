package com.ismartcoding.plain.ui

import android.os.Bundle
import android.view.Menu
import android.view.View
import com.ismartcoding.lib.brv.utils.*
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.plain.LocalStorage
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.data.AllItemsOption
import com.ismartcoding.plain.data.UIDataCache
import com.ismartcoding.plain.databinding.DialogSelectItemBinding
import com.ismartcoding.plain.databinding.ViewListItemBinding
import com.ismartcoding.plain.features.device.*
import com.ismartcoding.plain.features.network.bindNetwork
import com.ismartcoding.plain.features.ApplyToType
import com.ismartcoding.plain.features.box.FetchNetworksEvent
import com.ismartcoding.plain.features.box.NetworksResultEvent
import com.ismartcoding.plain.fragment.DeviceFragment
import com.ismartcoding.plain.fragment.NetworkFragment
import com.ismartcoding.plain.ui.extensions.*
import com.ismartcoding.plain.ui.helpers.DeviceSortHelper
import com.ismartcoding.plain.ui.helpers.DialogHelper

class SelectItemDialog(val search: (String) -> List<Any>, val onSelect: (ApplyToType, String) -> Unit) : BaseBottomSheetDialog<DialogSelectItemBinding>() {
    private var searchQ: String = ""
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.topAppBar.run {
            initMenu(R.menu.select_item)

            DeviceSortHelper.getSelectedSortItem(menu).highlightTitle(requireContext())

            onMenuItemClick {
                when (itemId) {
                    R.id.sort_name_asc -> {
                        sort(menu, DeviceSortBy.NAME_ASC)
                    }
                    R.id.sort_name_desc -> {
                        sort(menu, DeviceSortBy.NAME_DESC)
                    }
                    R.id.sort_ip_address -> {
                        sort(menu, DeviceSortBy.IP_ADDRESS)
                    }
                    R.id.sort_last_active_desc -> {
                        sort(menu, DeviceSortBy.LAST_ACTIVE)
                    }
                }
            }

            onSearch { q ->
                if (searchQ != q) {
                    searchQ = q
                    updateList()
                }
            }
        }

        binding.list.rv.isNestedScrollingEnabled = false
        binding.list.rv.linear().setup {
            addType<DeviceFragment>(R.layout.view_list_item)
            addType<AllItemsOption>(R.layout.view_list_item)
            addType<NetworkFragment>(R.layout.view_list_item)
            onBind {
                val binding = getBinding<ViewListItemBinding>()
                val m = getModel<Any>()
                var value = ""
                var type = ApplyToType.ALL
                when (m) {
                    is DeviceFragment -> {
                        type = ApplyToType.DEVICE
                        value = m.mac
                        binding.bindDevice(requireContext(), m)
                    }
                    is AllItemsOption -> {
                        value = ""
                        binding.setKeyText(m.name)
                        binding.clearTextRows()
                    }
                    is NetworkFragment -> {
                        type = ApplyToType.INTERFACE
                        value = m.ifName
                        binding.bindNetwork(requireContext(), m)
                    }
                }
                binding.setClick {
                    onSelect(type, value)
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
            updateList()
        }

        if (UIDataCache.current().devices == null) {
            binding.list.page.showLoading()
        } else {
            updateList()
        }
    }


    private fun sort(menu: Menu, sortBy: DeviceSortBy) {
        DeviceSortHelper.getSelectedSortItem(menu).unhighlightTitle()
        LocalStorage.deviceSortBy = sortBy
        DeviceSortHelper.getSelectedSortItem(menu).highlightTitle(requireContext())
        updateList()
    }

    private fun updateList() {
        binding.list.page.replaceData(search(searchQ))
    }

    private fun fetch() {
        sendEvent(FetchNetworksEvent(TempData.selectedBoxId))
    }
}