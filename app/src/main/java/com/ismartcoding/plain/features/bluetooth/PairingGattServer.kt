package com.ismartcoding.plain.features.bluetooth

import com.ismartcoding.plain.helpers.JsonHelper
import com.ismartcoding.plain.data.DPairingRequest
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.MainApp
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

class PairingGattServer(
    private val requestProvider: () -> DPairingRequest,
    private val onPeerRequest: suspend (DPairingRequest) -> Unit,
) {
    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
        val PAIRING_CHAR_UUID: UUID = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val context: Context get() = MainApp.instance
    private val bluetoothManager get() = context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager

    private var advertiser: BluetoothLeAdvertiser? = null
    private var gattServer: BluetoothGattServer? = null

    fun start() {
        val adapter = bluetoothManager.adapter ?: return
        advertiser = adapter.bluetoothLeAdvertiser ?: return
        startAdvertising()
        openGattServer()
    }

    fun stop() {
        advertiser?.stopAdvertising(advertiseCallback)
        advertiser = null
        try { gattServer?.close() } catch (_: Exception) {}
        gattServer = null
    }

    private fun startAdvertising() {
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTimeout(0)
            .build()
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()
        try {
            advertiser?.startAdvertising(settings, data, advertiseCallback)
        } catch (e: Exception) {
            LogCat.e("GATT advertise error: ${e.message}")
        }
    }

    private fun openGattServer() {
        val server = try {
            bluetoothManager.openGattServer(context, gattCallback)
        } catch (e: Exception) {
            LogCat.e("GATT server open error: ${e.message}")
            null
        } ?: return

        val service = BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val characteristic = BluetoothGattCharacteristic(
            PAIRING_CHAR_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE,
        )
        service.addCharacteristic(characteristic)
        server.addService(service)
        gattServer = server
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            LogCat.d("GATT advertising started")
        }
        override fun onStartFailure(errorCode: Int) {
            LogCat.e("GATT advertising failed: $errorCode")
        }
    }

    private val gattCallback = object : BluetoothGattServerCallback() {
        override fun onCharacteristicReadRequest(
            device: android.bluetooth.BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic,
        ) {
            LogCat.d("GATT read request from ${device.address} for ${characteristic.uuid}")
            if (characteristic.uuid != PAIRING_CHAR_UUID) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
                return
            }
            val payload = JsonHelper.jsonEncode(requestProvider())
            val bytes = payload.toByteArray(Charsets.UTF_8)
            val chunk = if (offset < bytes.size) bytes.copyOfRange(offset, bytes.size) else ByteArray(0)
            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, chunk)
        }

        override fun onCharacteristicWriteRequest(
            device: android.bluetooth.BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray,
        ) {
            LogCat.d("GATT write request from ${device.address} for ${characteristic.uuid} bytes=${value.size}")
            if (characteristic.uuid != PAIRING_CHAR_UUID) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
                return
            }
            val json = String(value, Charsets.UTF_8)
            val request = try {
                JsonHelper.jsonDecode<DPairingRequest>(json)
            } catch (e: Exception) {
                LogCat.e("GATT write parse error: ${e.message}")
                null
            }
            if (request == null) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
                return
            }
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
            }
            scope.launch { onPeerRequest(request) }
        }
    }
}
