package com.ismartcoding.plain.ui.device

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.UpdateDeviceNameMutation
import com.ismartcoding.plain.api.BoxApi
import com.ismartcoding.plain.data.UIDataCache
import com.ismartcoding.plain.databinding.DialogDeviceBinding
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.features.DeviceNameUpdatedEvent
import com.ismartcoding.plain.features.device.getName
import com.ismartcoding.plain.fragment.DeviceFragment
import com.ismartcoding.plain.ui.BaseDialog
import com.ismartcoding.plain.ui.EditValueDialog
import com.ismartcoding.plain.ui.extensions.onBack
import com.ismartcoding.plain.ui.extensions.setScrollBehavior
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.views.ListItemView
import kotlinx.coroutines.launch

class DeviceDialog(private var mItem: DeviceFragment) : BaseDialog<DialogDeviceBinding>() {
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        updateUI()
    }

    private fun updateUI() {
        binding.topAppBar.setScrollBehavior(false)
        binding.topAppBar.toolbar.run {
            title = mItem.getName()
            onBack {
                dismiss()
            }
        }

        binding.page.onRefresh {
            binding.page.finishRefresh()
        }

        binding.info.run {
            removeAllViews()
            val context = requireContext()
            val nameRow = ListItemView.createItem(context, getString(R.string.name), mItem.getName())
            nameRow.showMore()
            nameRow.setClick {
                EditValueDialog(getString(R.string.rename), getString(R.string.name), mItem.getName()) {
                    lifecycleScope.launch {
                        blockFormUI()
                        val r = withIO { BoxApi.mixMutateAsync((UpdateDeviceNameMutation(mItem.id, binding.value.text.trim()))) }
                        unblockFormUI()
                        if (!r.isSuccess()) {
                            DialogHelper.showErrorDialog(r.getErrorMessage())
                            return@launch
                        }
                        dismiss()
                        r.response?.data?.updateDeviceName?.let {
                            UIDataCache.current().devices?.run {
                                val index = indexOfFirst { d -> d.id == mItem.id }
                                if (index != -1) {
                                    removeAt(index)
                                    add(index, it.deviceFragment)
                                } else {
                                    add(it.deviceFragment)
                                }
                            }
                            mItem = it.deviceFragment
                            updateUI()
                            sendEvent(DeviceNameUpdatedEvent(mItem.id, it.deviceFragment.name))
                        }
                    }
                }.show()
            }
            addView(nameRow)
            addView(ListItemView.createItem(context, getString(R.string.ip_address), mItem.ip4))
            addView(ListItemView.createItem(context, getString(R.string.mac_address), mItem.mac.uppercase()))
            addView(
                ListItemView.createItem(
                    context,
                    getString(R.string.manufacturer),
                    if (mItem.macVendor.isEmpty()) getString(R.string.unknown) else mItem.macVendor,
                ),
            )
            addView(
                ListItemView.createItem(
                    context,
                    getString(R.string.status),
                    if (mItem.isOnline) getString(R.string.online) else getString(R.string.offline),
                ),
            )
            addView(ListItemView.createItem(context, getString(R.string.created_at), mItem.createdAt.formatDateTime()))
            addView(ListItemView.createItem(context, getString(R.string.active_at), mItem.activeAt.formatDateTime()))
        }
    }
}
