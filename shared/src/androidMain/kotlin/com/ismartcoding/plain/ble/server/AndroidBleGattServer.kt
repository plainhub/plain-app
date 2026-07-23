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
import android.os.ParcelUuid
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.ble.BleSegmentData
import com.ismartcoding.plain.ble.BleServiceData
import com.ismartcoding.plain.ble.BleUuids
import com.ismartcoding.plain.helpers.JsonHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.isWifiAwareSupported
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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

    /**
     * Per-MAC deferred for `onNotificationSent` flow control. Only one
     * notification per device is in flight at a time —
     * [sendNotificationBlocking] awaits this before sending the next chunk.
     */
    private val pendingNotificationAcks = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()

    /**
     * Notification chunk size for response delivery. Matches the request
     * segment size used by [com.ismartcoding.plain.ble.client.BleDeviceApi]
     * so both directions share the same BLE MTU headroom.
     */
    private val notifyChunkSize = 380

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
        // Do NOT include the device name in the advertising packet — on some
        // devices (e.g. Pixel 9 / Android 17) the system adds extra bytes
        // (TX power, flags) that push the total past the 31-byte limit,
        // causing ADVERTISE_FAILED_DATA_TOO_LARGE (error code 1). The peer's
        // display name is fetched later via the GATT DISCOVER reply, so it
        // is not needed here. Keeping only the service UUID keeps the
        // advertising packet minimal and compatible with the ScanFilter.
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(UUID.fromString(BleUuids.SERVICE_UUID)))
            .build()
        val scanResponse = AdvertiseData.Builder()
            .addServiceData(
                ParcelUuid(UUID.fromString(BleUuids.SERVICE_UUID)),
                buildServiceData(),
            )
            .build()
        try {
            advertiser?.startAdvertising(settings, data, scanResponse, advertiseCallback)
        } catch (e: Exception) {
            LogCat.e("GATT advertise error: ${e.message}")
        }
    }

    /**
     * Builds the scan response serviceData payload: byte[0] = aware flags,
     * byte[1..8] = SHA256(clientId)[0:8]. Total 9 bytes — well within the
     * 31-byte BLE limit (full entry = 1 len + 1 type + 16 UUID + 9 = 27 bytes).
     * See [BleServiceData] for the wire format.
     */
    private fun buildServiceData(): ByteArray =
        BleServiceData.encode(
            awareSupported = isWifiAwareSupported,
            awareRunning = TempData.awareRunning.value,
            clientId = TempData.clientId,
        )

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
            // Responses are now delivered via chunked notifications — see
            // [sendChunkedResponse]. Keep a no-op read response so legacy
            // clients that still issue a Read Request get GATT_SUCCESS with
            // an empty payload instead of an error.
            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, ByteArray(0))
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
                val response = protocol.handleWrite(mac, charUuid, value)
                if (response != null) {
                    sendChunkedResponse(device, characteristic.uuid, response)
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
            pendingNotificationAcks.remove(device.address)?.complete(status == BluetoothGatt.GATT_SUCCESS)
        }
    }

    /**
     * Chunk [response] into [BleSegmentData] notifications and send each one
     * to the peer with flow control. The client reassembles the segments into
     * the original response string. This bypasses the 512-byte ATT attribute
     * value limit that truncates large `readCharacteristic` responses.
     */
    @Suppress("DEPRECATION")
    private suspend fun sendChunkedResponse(
        device: android.bluetooth.BluetoothDevice,
        charUuid: UUID,
        response: String,
    ) {
        val mac = device.address
        val server = gattServer
        val char = server?.getService(UUID.fromString(BleUuids.SERVICE_UUID))?.getCharacteristic(charUuid)
        if (server == null || char == null) {
            LogCat.e("[GATT] sendChunkedResponse mac=$mac charUuid=$charUuid: server/char unavailable")
            return
        }

        val chunks = if (response.isEmpty()) listOf("") else response.chunked(notifyChunkSize)
        LogCat.d("[GATT] sendChunkedResponse mac=$mac charUuid=$charUuid responseLen=${response.length} chunks=${chunks.size}")

        for ((index, chunk) in chunks.withIndex()) {
            val segment = BleSegmentData.build(
                data = chunk,
                start = index == 0,
                end = index == chunks.lastIndex,
            )
            val payload = JsonHelper.jsonEncode(segment)
            val ok = sendNotificationBlocking(mac, charUuid.toString(), payload)
            if (!ok) {
                LogCat.e("[GATT] sendChunkedResponse mac=$mac chunk $index/${chunks.size} failed, aborting")
                return
            }
        }
    }

    @Suppress("DEPRECATION")
    override suspend fun sendNotificationBlocking(mac: String, charUuid: String, value: String): Boolean {
        val server = gattServer ?: return false
        val device = connectedDevices[mac] ?: return false
        val char = server.getService(UUID.fromString(BleUuids.SERVICE_UUID))
            ?.getCharacteristic(UUID.fromString(charUuid)) ?: return false

        val ack = CompletableDeferred<Boolean>()
        pendingNotificationAcks[mac] = ack

        char.value = value.toByteArray(Charsets.UTF_8)
        val queued = server.notifyCharacteristicChanged(device, char, false)
        if (!queued) {
            pendingNotificationAcks.remove(mac)
            return false
        }

        val ok = withTimeoutOrNull(NOTIFY_ACK_TIMEOUT_MS) { ack.await() } ?: false
        // Only the ack owner should remove the entry; the timeout path clears it here.
        pendingNotificationAcks.remove(mac)
        return ok
    }

    companion object {
        private const val NOTIFY_ACK_TIMEOUT_MS = 10_000L
    }
}
