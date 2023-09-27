package com.ismartcoding.plain.ui.box

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.lib.softinput.setWindowSoftInput
import com.ismartcoding.plain.InitQuery
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.data.*
import com.ismartcoding.plain.databinding.DialogConnectBoxBinding
import com.ismartcoding.plain.features.bluetooth.*
import com.ismartcoding.plain.features.box.BoxHelper
import com.ismartcoding.plain.ui.BaseBottomSheetDialog
import com.ismartcoding.plain.ui.extensions.setSafeClick
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.launch
import org.json.JSONObject

class ConnectBoxDialog(private val device: BTDevice, val updateCallback: () -> Unit) : BaseBottomSheetDialog<DialogConnectBoxBinding>() {
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.topAppBar.title = device.device.name
        binding.connect.run {
            setSafeClick {
                if (hasInputError()) {
                    return@setSafeClick
                }

                val password = binding.password.text.trim()
                if (password.isEmpty()) {
                    DialogHelper.showMessage(R.string.empty_password)
                    return@setSafeClick
                }
                showLoading()
                lifecycleScope.launch {
                    val smartDevice = SmartBTDevice(device)
                    withIO {
                        smartDevice.ensureConnectedAsync()
                    }
                    if (!smartDevice.isConnected()) {
                        hideLoading()
                        DialogHelper.showMessage(R.string.bluetooth_error_message)
                        return@launch
                    }

                    val r = withIO { requestTokenAsync(smartDevice, password) }
                    if (!r.isSuccess()) {
                        finishRequestAsync(smartDevice, getString(R.string.invalid_password))
                        return@launch
                    }

                    val json = JSONObject(r.value as String)
                    val token = json.optString("token")
                    if (token.isEmpty()) {
                        finishRequestAsync(smartDevice, getString(R.string.invalid_password))
                        return@launch
                    }
                    bindBoxDataAsync(smartDevice, json)
                }
            }
        }

        addFormItem(binding.password)

        setWindowSoftInput(binding.connect)
    }

    private suspend fun requestTokenAsync(
        smartDevice: SmartBTDevice,
        password: String,
    ): BluetoothResult {
        val data = BleRequestData.create(requireContext())
        data.body = BleAuthData(CryptoHelper.sha512(password.toByteArray())).toJSON().toString()
        return smartDevice.requestAsync(BTDevice.authService, data)
    }

    private suspend fun bindBoxDataAsync(
        smartDevice: SmartBTDevice,
        json: JSONObject,
    ) {
        val boxId = json.optString("box_id")
        val token = json.optString("token")
        val r2 = withIO { smartDevice.graphqlAsync(requireContext(), InitQuery(), token) }
        if (r2 != null) {
            val ips =
                if (r2.data?.interfaces != null) {
                    BoxHelper.getIPs(
                        r2.data?.interfaces?.map {
                            it.interfaceFragment
                        } ?: arrayListOf(),
                    )
                } else {
                    arrayListOf()
                }
            withIO {
                BoxHelper.addOrUpdateAsync(boxId) { item ->
                    item.token = token
                    item.bluetoothMac = device.mac
                    item.name = smartDevice.name
                    item.ips.clear()
                    item.ips.addAll(ips)
                }
                if (TempData.selectedBoxId.isEmpty()) {
                    TempData.selectedBoxId = boxId
                }
                UIDataCache.current().box = BoxHelper.getSelectedBoxAsync()
            }
            finishRequestAsync(smartDevice, getString(R.string.box_added))
            dismiss()
            updateCallback()
        } else {
            finishRequestAsync(smartDevice, getString(R.string.invalid_password))
        }
    }

    private suspend fun finishRequestAsync(
        smartDevice: SmartBTDevice,
        error: String,
    ) {
        withIO { smartDevice.disconnect() }
        binding.connect.hideLoading()
        DialogHelper.showMessage(error)
    }
}
