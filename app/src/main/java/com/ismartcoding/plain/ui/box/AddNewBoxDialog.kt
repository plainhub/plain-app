package com.ismartcoding.plain.ui.box

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.brv.utils.addModels
import com.ismartcoding.lib.brv.utils.linear
import com.ismartcoding.lib.brv.utils.models
import com.ismartcoding.lib.brv.utils.setup
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.databinding.DialogAddNewBoxBinding
import com.ismartcoding.plain.features.bluetooth.*
import com.ismartcoding.plain.ui.BaseDialog
import com.ismartcoding.plain.ui.extensions.*
import com.ismartcoding.plain.ui.models.ListItemModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.launch

class AddNewBoxDialog(val updateCallback: () -> Unit) : BaseDialog<DialogAddNewBoxBinding>() {
    private var addedMacSet = mutableSetOf<String>()

    data class ItemModel(val device: BTDevice) : ListItemModel()

    @SuppressLint("MissingPermission")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.topAppBar.onBack {
            dismiss()
        }

        binding.rv.linear().setup {
            addType<ItemModel>(R.layout.item_row)
            R.id.container.onClick {
                val m = getModel<ItemModel>()
                ConnectBoxDialog(m.device) {
                    dismiss()
                    updateCallback()
                }.show()
            }
        }

        receiveEvent<BTDeviceFoundEvent> { event ->
            val device = event.device
            if (addedMacSet.contains(device.mac)) {
                return@receiveEvent
            }

            addedMacSet.add(device.mac)

            binding.rv.addModels(
                arrayListOf(
                    ItemModel(device).apply {
                        keyText = device.device.name
                        subtitle = device.mac
                    },
                ),
            )
            binding.discovering.visibility = View.GONE
            binding.empty.visibility = View.GONE
        }

        receiveEvent<ScanBTDeviceTimeoutEvent> {
            withIO {
                BluetoothUtil.stopScan()
            }
            binding.discovering.visibility = View.GONE
            if (binding.rv.models.isNullOrEmpty()) {
                binding.empty.run {
                    visibility = View.VISIBLE
                    setButton(getString(R.string.try_again)) {
                        startSearchingBTDevices()
                        binding.empty.visibility = View.GONE
                        binding.discovering.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        BluetoothUtil.stopScanAndRelease()
    }

    override fun onStart() {
        super.onStart()
        startSearchingBTDevices()
    }

    private fun startSearchingBTDevices() {
        lifecycleScope.launch {
            if (!withIO { BluetoothUtil.ensurePermissionAsync() }) {
                return@launch
            }
            launch {
                delay(10000)
                sendEvent(ScanBTDeviceTimeoutEvent())
            }

            BluetoothUtil.scan().buffer().collect { d ->
                sendEvent(BTDeviceFoundEvent(d))
            }
        }
    }
}
