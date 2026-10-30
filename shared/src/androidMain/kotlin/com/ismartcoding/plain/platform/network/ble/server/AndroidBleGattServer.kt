package com.ismartcoding.plain.platform.network.ble.server

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
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.platform.network.ble.BleMessage
import com.ismartcoding.plain.platform.network.ble.RustBleWire
import com.ismartcoding.plain.platform.network.ble.BleUuids
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.sync.Mutex

class AndroidBleGattServer : BleGattServer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val protocol = BleServerProtocol()

    private val bluetoothManager get() =
        appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    private val advertisingLock = Any()
    @Volatile private var closed = false
    private var advertisingRevision = 0L
    private var advertiseCallback: AdvertiseCallback? = null
    private var advertiser: BluetoothLeAdvertiser? = null
    private var gattServer: BluetoothGattServer? = null
    private val connectedDevices = ConcurrentHashMap<String, BluetoothDevice>()

    /**
     * Per-MAC deferred for `onNotificationSent` flow control. Only one
     * notification per device is in flight at a time —
     * [sendNotificationBlocking] awaits this before sending the next chunk.
     */
    private val pendingNotificationAcks = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()

    private val mtus = ConcurrentHashMap<String, Int>()
    private val sending = ConcurrentHashMap<String, Mutex>()

    override fun start(payload: ByteArray, revision: Long) {
        synchronized(advertisingLock) {
            check(!closed && revision > advertisingRevision) { "BLE advertiser closed or stale" }
            require(payload.size == 9)
            advertisingRevision = revision
            val adapter = checkNotNull(bluetoothManager.adapter) { "Bluetooth unavailable" }
            advertiser = checkNotNull(adapter.bluetoothLeAdvertiser) { "BLE advertising unavailable" }
            openGattServer()
            startAdvertising(payload)
        }
    }

    override fun stop() {
        synchronized(advertisingLock) {
            closed = true
            try { advertiseCallback?.let { advertiser?.stopAdvertising(it) } }
            catch (error: Exception) { LogCat.e("BLE advertising stop", error) }
            advertiser = null
            advertiseCallback = null
            try { gattServer?.close() }
            catch (error: Exception) { LogCat.e("BLE GATT close", error) }
            gattServer = null
        }
        protocol.clear()
        mtus.clear()
        connectedDevices.clear()
        pendingNotificationAcks.values.forEach { it.complete(false) }
        pendingNotificationAcks.clear()
    }

    override fun refreshAdvertising(payload: ByteArray, revision: Long) {
        synchronized(advertisingLock) {
            check(!closed && revision > advertisingRevision) { "BLE advertiser closed or stale" }
            require(payload.size == 9)
            advertisingRevision = revision
            advertiseCallback?.let { advertiser?.stopAdvertising(it) }
            advertiseCallback = null
            startAdvertising(payload)
        }
    }

    @Suppress("DEPRECATION")
    override fun sendNotification(mac: String, charUuid: String, value: ByteArray): Boolean {
        if (closed) return false
        val requestId = protocol.nearbyRequestId(mac) ?: return false
        val device = connectedDevices[mac] ?: return false
        scope.launch {
            try { sendChunkedResponse(device, UUID.fromString(charUuid), BleMessage(requestId, RustBleWire.nearby(value))) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { LogCat.e("GATT notification error: ${error.message}") }
        }
        return true
    }

    private fun startAdvertising(payload: ByteArray) {
        val target = checkNotNull(advertiser) { "BLE advertising unavailable" }
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true).setTimeout(0).build()
        val uuid = ParcelUuid(UUID.fromString(BleUuids.SERVICE_UUID))
        val data = AdvertiseData.Builder().setIncludeDeviceName(false).addServiceUuid(uuid).build()
        val scanResponse = AdvertiseData.Builder().addServiceData(uuid, payload.copyOf()).build()
        val callback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                synchronized(advertisingLock) {
                    if (advertiseCallback === this) LogCat.d("GATT advertising started")
                }
            }
            override fun onStartFailure(errorCode: Int) {
                synchronized(advertisingLock) {
                    if (advertiseCallback === this) {
                        advertiseCallback = null
                        LogCat.e("GATT advertising failed: $errorCode")
                    }
                }
            }
        }
        advertiseCallback = callback
        target.startAdvertising(settings, data, scanResponse, callback)
    }

    private fun openGattServer() {
        val server = try {
            bluetoothManager.openGattServer(appContext, gattCallback)
        } catch (e: Exception) {
            LogCat.e("GATT server open error: ${e.message}")
            null
        } ?: error("BLE GATT server unavailable")

        gattServer = server
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

        check(server.addService(service)) { "BLE service registration failed" }
    }

    private val gattCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(
            device: BluetoothDevice,
            status: Int,
            newState: Int,
        ) {
            if (closed) return
            LogCat.d("[GATT] onConnectionStateChange mac=${device.address} status=$status newState=$newState")
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedDevices[device.address] = device
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    mtus.remove(device.address)
                    connectedDevices.remove(device.address)
                    pendingNotificationAcks.remove(device.address)?.complete(false)
                    protocol.clearClient(device.address)
                }
            }
        }

        override fun onMtuChanged(device: BluetoothDevice, mtu: Int) { if (!closed) mtus[device.address] = mtu }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic,
        ) {
            if (closed) return
            // Responses are delivered through notifications.
            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, ByteArray(0))
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray,
        ) {
            if (closed) return
            val charUuid = characteristic.uuid.toString()
            val mac = device.address
            LogCat.d("[GATT] onWriteRequest mac=$mac charUuid=$charUuid offset=$offset valueSize=${value.size} responseNeeded=$responseNeeded")
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
            }

            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    val response = protocol.handleWrite(mac, charUuid, value)
                    if (response != null) sendChunkedResponse(device, characteristic.uuid, response)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) { protocol.clearClient(mac); LogCat.e("BLE exchange failed: ${error.message}") }
            }
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray,
        ) {
            if (closed) return
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
            device: BluetoothDevice,
            status: Int,
        ) {
            LogCat.d("GATT notification sent to ${device.address} status=$status")
            pendingNotificationAcks.remove(device.address)?.complete(status == BluetoothGatt.GATT_SUCCESS)
        }
    }

    private suspend fun sendChunkedResponse(device: BluetoothDevice, charUuid: UUID, response: BleMessage) {
        val mac = device.address
        sending.getOrPut(mac) { Mutex() }.withLock {
            val limit = minOf((mtus[mac] ?: 23) - 3, 512)
            val encoder = RustBleWire.encoder(response.bytes, response.requestId, true, limit)
            try {
                while (connectedDevices[mac] === device) {
                    val frame = encoder.next() ?: break
                    check(sendNotificationBlocking(mac, charUuid.toString(), frame)) { "BLE response notification failed" }
                }
            } finally { encoder.close() }
        }
    }

    @Suppress("DEPRECATION")
    override suspend fun sendNotificationBlocking(mac: String, charUuid: String, value: ByteArray): Boolean {
        if (closed) return false
        val server = gattServer ?: return false
        val device = connectedDevices[mac] ?: return false
        val char = server.getService(UUID.fromString(BleUuids.SERVICE_UUID))
            ?.getCharacteristic(UUID.fromString(charUuid)) ?: return false

        val ack = CompletableDeferred<Boolean>()
        pendingNotificationAcks[mac] = ack

        char.value = value
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
