package com.ismartcoding.plain.ble.server

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelUuid
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.ble.BleUuids
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class AndroidBleGattServer : BleGattServer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val protocol = BleServerProtocol()

    private val bluetoothManager get() =
        appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    private var advertiser: BluetoothLeAdvertiser? = null
    private var gattServer: BluetoothGattServer? = null
    private val connectedDevices = ConcurrentHashMap<String, android.bluetooth.BluetoothDevice>()

    override fun start() {
        val adapter = bluetoothManager.adapter ?: return
        advertiser = adapter.bluetoothLeAdvertiser ?: return
        startAdvertising()
        openGattServer()
    }

    override fun stop() {
        advertiser?.stopAdvertising(advertiseCallback)
        advertiser = null
        try {
            gattServer?.close()
        } catch (_: Exception) {
        }
        gattServer = null
    }

    override fun refreshAdvertising() {
        advertiser?.stopAdvertising(advertiseCallback)
        startAdvertising()
    }

    @Suppress("DEPRECATION")
    override fun sendNotification(mac: String, charUuid: String, value: String): Boolean {
        val server = gattServer ?: run {
            LogCat.e("[GATT] sendNotification: gattServer is null")
            return false
        }
        val device = connectedDevices[mac] ?: run {
            LogCat.e("[GATT] sendNotification: device $mac not connected")
            return false
        }
        val char = server.getService(UUID.fromString(BleUuids.SERVICE_UUID))
            ?.getCharacteristic(UUID.fromString(charUuid)) ?: run {
            LogCat.e("[GATT] sendNotification: characteristic $charUuid not found")
            return false
        }
        char.value = value.toByteArray(Charsets.UTF_8)
        val sent = server.notifyCharacteristicChanged(device, char, false)
        LogCat.d("[GATT] sendNotification mac=$mac charUuid=$charUuid valueSize=${value.length} sent=$sent")
        return sent
    }

    private fun startAdvertising() {
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTimeout(0)
            .build()
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(ParcelUuid(UUID.fromString(BleUuids.SERVICE_UUID)))
            .build()
        val scanResponse = AdvertiseData.Builder()
            .addServiceData(
                ParcelUuid(UUID.fromString(BleUuids.SERVICE_UUID)),
                byteArrayOf(buildAwareFlags()),
            )
            .build()
        try {
            advertiser?.startAdvertising(settings, data, scanResponse, advertiseCallback)
        } catch (e: Exception) {
            LogCat.e("GATT advertise error: ${e.message}")
        }
    }

    private fun buildAwareFlags(): Byte {
        var flags = 0
        if (isAwareSupported()) flags = flags or FLAG_AWARE_SUPPORTED
        if (TempData.awareRunning.value) flags = flags or FLAG_AWARE_RUNNING
        return flags.toByte()
    }

    private fun isAwareSupported(): Boolean {
        return appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)
    }

    private fun openGattServer() {
        val server = try {
            bluetoothManager.openGattServer(appContext, gattCallback)
        } catch (e: Exception) {
            LogCat.e("GATT server open error: ${e.message}")
            null
        } ?: return

        val service = BluetoothGattService(
            UUID.fromString(BleUuids.SERVICE_UUID),
            BluetoothGattService.SERVICE_TYPE_PRIMARY,
        )

        for (handler in protocol.handlers) {
            val charUuid = UUID.fromString(handler.charUuid)
            val char = BluetoothGattCharacteristic(
                charUuid,
                BluetoothGattCharacteristic.PROPERTY_READ or
                    BluetoothGattCharacteristic.PROPERTY_WRITE or
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ or
                    BluetoothGattCharacteristic.PERMISSION_WRITE,
            )
            char.addDescriptor(
                BluetoothGattDescriptor(
                    UUID.fromString(BleUuids.CCC_DESCRIPTOR_UUID),
                    BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE,
                ),
            )
            service.addCharacteristic(char)
        }

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
        override fun onConnectionStateChange(
            device: android.bluetooth.BluetoothDevice,
            status: Int,
            newState: Int,
        ) {
            LogCat.d("[GATT] onConnectionStateChange mac=${device.address} status=$status newState=$newState")
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedDevices[device.address] = device
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    connectedDevices.remove(device.address)
                    protocol.clearClient(device.address)
                }
            }
        }

        override fun onCharacteristicReadRequest(
            device: android.bluetooth.BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic,
        ) {
            val mac = device.address
            val charUuid = characteristic.uuid.toString()
            val payload = protocol.handleRead(mac, charUuid, offset)
            LogCat.d("[GATT] onReadRequest mac=$mac charUuid=$charUuid offset=$offset payloadSize=${payload.size}")
            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, payload)
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
            val charUuid = characteristic.uuid.toString()
            val mac = device.address
            LogCat.d("[GATT] onWriteRequest mac=$mac charUuid=$charUuid offset=$offset valueSize=${value.size} responseNeeded=$responseNeeded")
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
            }

            scope.launch {
                val processed = protocol.handleWrite(mac, charUuid, value)
                LogCat.d("[GATT] handleWrite mac=$mac charUuid=$charUuid processed=$processed")
                if (processed) {
                    notifyReady(device, characteristic.uuid)
                }
            }
        }

        override fun onDescriptorWriteRequest(
            device: android.bluetooth.BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray,
        ) {
            val mac = device.address
            val charUuid = descriptor.characteristic.uuid.toString()
            val descUuid = descriptor.uuid.toString()
            val valueHex = value.joinToString("") { "%02x".format(it) }
            LogCat.d("[GATT] onDescriptorWriteRequest mac=$mac charUuid=$charUuid descUuid=$descUuid offset=$offset value=$valueHex responseNeeded=$responseNeeded")
            if (descriptor.uuid == UUID.fromString(BleUuids.CCC_DESCRIPTOR_UUID) && responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value)
                LogCat.d("[GATT] onDescriptorWriteRequest mac=$mac: sent CCCD response")
            }
        }

        override fun onServiceAdded(status: Int, service: BluetoothGattService) {
            LogCat.d("[GATT] onServiceAdded status=$status service=${service.uuid}")
        }

        override fun onNotificationSent(
            device: android.bluetooth.BluetoothDevice,
            status: Int,
        ) {
            LogCat.d("GATT notification sent to ${device.address} status=$status")
        }
    }

    @Suppress("DEPRECATION")
    private fun notifyReady(device: android.bluetooth.BluetoothDevice, charUuid: UUID) {
        val server = gattServer ?: return
        val char = server.getService(UUID.fromString(BleUuids.SERVICE_UUID))
            ?.getCharacteristic(charUuid) ?: return
        char.value = READY_SIGNAL.toByteArray(Charsets.UTF_8)
        server.notifyCharacteristicChanged(device, char, false)
    }

    companion object {
        private const val READY_SIGNAL = "1"
        private const val FLAG_AWARE_SUPPORTED = 0x01
        private const val FLAG_AWARE_RUNNING = 0x02
    }
}
