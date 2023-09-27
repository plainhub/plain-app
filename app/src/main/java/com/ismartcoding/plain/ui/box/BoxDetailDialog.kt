package com.ismartcoding.plain.ui.box

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.InitQuery
import com.ismartcoding.plain.R
import com.ismartcoding.plain.api.BoxApi
import com.ismartcoding.plain.data.UIDataCache
import com.ismartcoding.plain.databinding.DialogBoxDetailBinding
import com.ismartcoding.plain.db.DBox
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.features.box.BoxHelper
import com.ismartcoding.plain.ui.BaseDialog
import com.ismartcoding.plain.ui.extensions.onBack
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.views.ListItemView
import kotlinx.coroutines.launch

class BoxDetailDialog(private val boxId: String) : BaseDialog<DialogBoxDetailBinding>() {
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        updateUI()
    }

    private fun updateUI() {
        lifecycleScope.launch {
            val item = withIO { BoxHelper.getItemsAsync().find { it.id == boxId } } ?: return@launch
            binding.topAppBar.run {
                title = item.name
                onBack {
                    dismiss()
                }
            }

            binding.refresh.setOnRefreshListener {
                fetch(item)
            }

            binding.boxInfo.run {
                removeAllViews()
                val context = requireContext()
                addView(ListItemView.createItem(context, getString(R.string.box_id), item.id))
                addView(ListItemView.createItem(context, getString(R.string.client_token), item.token))
                addView(ListItemView.createItem(context, getString(R.string.ip_address), item.ips.joinToString(", ")))
                addView(ListItemView.createItem(context, getString(R.string.bluetooth_mac), item.bluetoothMac))
                addView(ListItemView.createItem(context, getString(R.string.added_at), item.createdAt.formatDateTime()))
                addView(ListItemView.createItem(context, getString(R.string.updated_at), item.updatedAt.formatDateTime()))
            }
        }
    }

    private fun fetch(boxItem: DBox) {
        lifecycleScope.launch {
            val r = withIO { BoxApi.mixQueryAsync(InitQuery(), boxItem) }
            if (!r.isSuccess()) {
                DialogHelper.showMessage(r)
                binding.refresh.finishRefresh(false)
                return@launch
            }

            withIO {
                val ips =
                    if (r.response?.data?.network != null) {
                        BoxHelper.getIPs(
                            r.response.data!!.interfaces.map {
                                it.interfaceFragment
                            },
                        )
                    } else {
                        arrayListOf()
                    }
                if (ips.isNotEmpty()) {
                    BoxHelper.addOrUpdateAsync(boxId) { item ->
                        item.ips.clear()
                        item.ips.addAll(ips)
                    }
                    UIDataCache.current().box = BoxHelper.getSelectedBoxAsync()
                }
            }

            updateUI()
            binding.refresh.finishRefresh(r.isRealSuccess())
        }
    }
}
