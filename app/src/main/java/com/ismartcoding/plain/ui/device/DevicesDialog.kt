package com.ismartcoding.plain.ui.device

import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.brv.utils.linear
import com.ismartcoding.lib.brv.utils.setup
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.DeleteDeviceMutation
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.api.BoxApi
import com.ismartcoding.plain.data.UIDataCache
import com.ismartcoding.plain.data.enums.ActionSourceType
import com.ismartcoding.plain.data.enums.ActionType
import com.ismartcoding.plain.data.preference.DeviceSortByPreference
import com.ismartcoding.plain.databinding.DialogDevicesBinding
import com.ismartcoding.plain.databinding.ViewListItemBinding
import com.ismartcoding.plain.extensions.sorted
import com.ismartcoding.plain.features.ActionEvent
import com.ismartcoding.plain.features.DeviceNameUpdatedEvent
import com.ismartcoding.plain.features.box.FetchNetworksEvent
import com.ismartcoding.plain.features.box.NetworksResultEvent
import com.ismartcoding.plain.features.device.DeviceSortBy
import com.ismartcoding.plain.features.device.bindDevice
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.fragment.DeviceFragment
import com.ismartcoding.plain.ui.BaseDialog
import com.ismartcoding.plain.ui.extensions.enableSwipeMenu
import com.ismartcoding.plain.ui.extensions.highlightTitle
import com.ismartcoding.plain.ui.extensions.initMenu
import com.ismartcoding.plain.ui.extensions.onBack
import com.ismartcoding.plain.ui.extensions.onMenuItemClick
import com.ismartcoding.plain.ui.extensions.onSearch
import com.ismartcoding.plain.ui.extensions.setRightSwipeButton
import com.ismartcoding.plain.ui.extensions.setScrollBehavior
import com.ismartcoding.plain.ui.extensions.unhighlightTitle
import com.ismartcoding.plain.ui.helpers.DeviceSortHelper
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.launch

class DevicesDialog : BaseDialog<DialogDevicesBinding>() {
    private var searchQ: String = ""

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        binding.list.rv.linear().setup {
            addType<DeviceFragment>(R.layout.view_list_item)
            onBind {
                val binding = getBinding<ViewListItemBinding>()
                val m = getModel<DeviceFragment>()
                binding.bindDevice(requireContext(), m)
                binding.enableSwipeMenu(true)
                binding.setRightSwipeButton(getString(R.string.delete)) {
                    DialogHelper.confirmToAction(requireContext(), R.string.confirm_to_delete) {
                        lifecycleScope.launch {
                            DialogHelper.showLoading()
                            val r = withIO { BoxApi.mixMutateAsync(DeleteDeviceMutation(m.id)) }
                            DialogHelper.hideLoading()
                            if (!r.isSuccess()) {
                                DialogHelper.showErrorDialog(requireContext(), r.getErrorMessage())
                                return@launch
                            }

                            UIDataCache.current().devices?.removeAll { it.id == m.id }
                            search()
                            sendEvent(ActionEvent(ActionSourceType.DEVICE, ActionType.DELETED, setOf(m.id)))
                        }
                    }
                }
            }
        }

        binding.topAppBar.setScrollBehavior(false)
        binding.topAppBar.toolbar.run {
            setTitle(R.string.devices)
            initMenu(R.menu.devices)

            onBack {
                dismiss()
            }

            lifecycleScope.launch {
                DeviceSortHelper.getSelectedSortItemAsync(requireContext(), menu).highlightTitle(requireContext())
            }

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
                    search()
                }
            }
        }

        binding.list.page.setOnRefreshListener {
            fetch()
        }

        receiveEvent<DeviceNameUpdatedEvent> {
            search()
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

        if (UIDataCache.current().devices == null) {
            binding.list.page.showLoading()
        } else {
            search()
        }
    }

    private fun sort(
        menu: Menu,
        sortBy: DeviceSortBy,
    ) {
        lifecycleScope.launch {
            DeviceSortHelper.getSelectedSortItemAsync(requireContext(), menu).unhighlightTitle()
            withIO { DeviceSortByPreference.putAsync(requireContext(), sortBy) }
            DeviceSortHelper.getSelectedSortItemAsync(requireContext(), menu).highlightTitle(requireContext())
            search()
        }
    }

    private fun search() {
        lifecycleScope.launch {
            val devices = UIDataCache.current().getDevices(searchQ)
            val sort = withIO { DeviceSortByPreference.getValueAsync(requireContext()) }
            binding.list.page.replaceData(devices.sorted(sort))
            binding.topAppBar.toolbar.run {
                title = LocaleHelper.getString(R.string.devices)
                val total = devices.size
                subtitle =
                    if (total > 0) {
                        LocaleHelper.getStringF(R.string.devices_subtitle, "total", total, "online", devices.count { it.isOnline })
                    } else {
                        ""
                    }
            }
        }
    }

    private fun fetch() {
        sendEvent(FetchNetworksEvent(TempData.selectedBoxId))
    }
}
