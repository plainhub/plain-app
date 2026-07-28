@file:OptIn(ExperimentalForeignApi::class)

package com.ismartcoding.plain.ble.server

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.ble.BleSegmentData
import com.ismartcoding.plain.ble.BleServiceData
import com.ismartcoding.plain.ble.BleUuids
import com.ismartcoding.plain.helpers.JsonHelper
import com.ismartcoding.plain.lib.toByteArray
import com.ismartcoding.plain.lib.toNSData
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.isWifiAwareSupported
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreBluetooth.CBATTErrorSuccess
import platform.CoreBluetooth.CBATTRequest
import platform.CoreBluetooth.CBAttributePermissionsReadable
import platform.CoreBluetooth.CBAttributePermissionsWriteable
import platform.CoreBluetooth.CBCharacteristicPropertyNotify
import platform.CoreBluetooth.CBCharacteristicPropertyRead
import platform.CoreBluetooth.CBCharacteristicPropertyWrite
import platform.CoreBluetooth.CBMutableCharacteristic
import platform.CoreBluetooth.CBMutableDescriptor
import platform.CoreBluetooth.CBMutableService
import platform.CoreBluetooth.CBPeripheralManager
import platform.CoreBluetooth.CBPeripheralManagerDelegateProtocol
import platform.CoreBluetooth.CBManagerStatePoweredOn
import platform.CoreBluetooth.CBUUID
import platform.CoreBluetooth.CBService
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import platform.darwin.NSObject
import kotlin.time.Duration.Companion.milliseconds

class IosBleGattServer : BleGattServer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val protocol = BleServerProtocol()

    private var peripheralManager: CBPeripheralManager? = null
    private var delegate: PeripheralManagerDelegate? = null
    private val characteristics = mutableMapOf<String, CBMutableCharacteristic>()
    private var serviceAdded = false
    private var advertising = false

    /**
     * Notification chunk size for response delivery. Matches the request
     * segment size used by [com.ismartcoding.plain.ble.client.BleDeviceApi].
     */
    private val notifyChunkSize = 380

    private val notifyAckTimeoutMs = 10_000L

    override fun start() {
        if (peripheralManager != null) return
        val del = PeripheralManagerDelegate(this)
        delegate = del
        peripheralManager = CBPeripheralManager(del, null)
    }

    override fun stop() {
        val manager = peripheralManager ?: return
        if (advertising) {
            manager.stopAdvertising()
            advertising = false
        }
        if (serviceAdded) {
            manager.removeAllServices()
            serviceAdded = false
        }
        peripheralManager = null
        delegate = null
        characteristics.clear()
    }

    override fun refreshAdvertising() {
        val manager = peripheralManager ?: return
        if (advertising) {
            manager.stopAdvertising()
        }
        startAdvertising(manager)
    }

    override fun sendNotification(mac: String, charUuid: String, value: String): Boolean {
        val manager = peripheralManager ?: return false
        val char = characteristics[charUuid] ?: run {
            LogCat.e("[BLE] sendNotification: characteristic $charUuid not found")
            return false
        }
        val data = value.encodeToByteArray().toNSData()
        val sent = manager.updateValue(data, forCharacteristic = char, onSubscribedCentrals = null)
        LogCat.d("[BLE] sendNotification charUuid=$charUuid valueSize=${value.length} sent=$sent")
        return sent
    }

    override suspend fun sendNotificationBlocking(mac: String, charUuid: String, value: String): Boolean {
        val manager = peripheralManager ?: return false
        val char = characteristics[charUuid] ?: return false
        val data = value.encodeToByteArray().toNSData()

        // iOS `updateValue` returns false when the internal queue is full.
        // Wait for `peripheralManagerIsReadyToUpdateSubscribers` and retry.
        var attempts = 0
        while (attempts < 10) {
            val deferred = CompletableDeferred<Unit>()
            // Stash the deferred so the delegate can complete it.
            pendingReadyDeferred.value = deferred
            val sent = manager.updateValue(data, forCharacteristic = char, onSubscribedCentrals = null)
            if (sent) {
                pendingReadyDeferred.value = null
                return true
            }
            // Wait for ready signal or timeout.
            val ready = withTimeoutOrNull(notifyAckTimeoutMs.milliseconds) { deferred.await() }
            pendingReadyDeferred.value = null
            if (ready == null) {
                LogCat.e("[BLE] sendNotificationBlocking: timed out waiting for readyToUpdate")
                return false
            }
            attempts++
        }
        LogCat.e("[BLE] sendNotificationBlocking: exhausted retries")
        return false
    }

    // @Volatile is JVM-only; use MutableStateFlow for thread-safe single-slot
    // state on Kotlin/Native (kotlin.native.concurrent.AtomicReference is
    // hard-deprecated in Kotlin 2.4).
    private val pendingReadyDeferred = MutableStateFlow<CompletableDeferred<Unit>?>(null)

    internal fun onReadyToUpdateSubscribers() {
        pendingReadyDeferred.value?.complete(Unit)
    }

    private fun setupService(manager: CBPeripheralManager) {
        val service = CBMutableService(CBUUID.UUIDWithString(BleUuids.SERVICE_UUID), true)

        val charList = mutableListOf<CBMutableCharacteristic>()
        for (handler in protocol.handlers) {
            val charUuid = CBUUID.UUIDWithString(handler.charUuid)
            val cccDescriptor = CBMutableDescriptor(
                CBUUID.UUIDWithString(BleUuids.CCC_DESCRIPTOR_UUID),
                null,
            )
            val char = CBMutableCharacteristic(
                charUuid,
                CBCharacteristicPropertyRead or CBCharacteristicPropertyWrite or CBCharacteristicPropertyNotify,
                null,
                CBAttributePermissionsReadable or CBAttributePermissionsWriteable,
            )
            char.setDescriptors(listOf(cccDescriptor))
            characteristics[handler.charUuid] = char
            charList.add(char)
        }

        service.setCharacteristics(charList)
        manager.addService(service)
    }

    private fun startAdvertising(manager: CBPeripheralManager) {
        val payload = BleServiceData.encode(
            awareSupported = isWifiAwareSupported,
            awareRunning = TempData.awareRunning.value,
            clientId = TempData.clientId,
        )
        val serviceDataMap: Map<Any?, Any?> = mapOf(
            CBUUID.UUIDWithString(BleUuids.SERVICE_UUID) to payload.toNSData(),
        )
        val advertisingData = mapOf<Any?, Any?>(
            platform.CoreBluetooth.CBAdvertisementDataServiceUUIDsKey to
                listOf(CBUUID.UUIDWithString(BleUuids.SERVICE_UUID)),
            platform.CoreBluetooth.CBAdvertisementDataServiceDataKey to serviceDataMap,
        )
        manager.startAdvertising(advertisingData)
        advertising = true
        LogCat.d("BLE GATT server advertising started (clientId=${TempData.clientId})")
    }

    internal fun onManagerReady() {
        val manager = peripheralManager ?: return
        if (!serviceAdded) {
            setupService(manager)
        }
    }

    internal fun onServiceAdded(error: NSError?) {
        if (error != null) {
            LogCat.e("BLE GATT server add service failed: ${error.localizedDescription}")
            return
        }
        serviceAdded = true
        val manager = peripheralManager ?: return
        if (!advertising) {
            startAdvertising(manager)
        }
    }

    internal fun onReadRequest(manager: CBPeripheralManager, request: CBATTRequest) {
        // Responses are now delivered via chunked notifications. Reply with
        // an empty value so legacy Read Requests still get a success status.
        request.value = ByteArray(0).toNSData()
        manager.respondToRequest(request, CBATTErrorSuccess)
    }

    internal fun onWriteRequests(manager: CBPeripheralManager, requests: List<CBATTRequest>) {
        for (request in requests) {
            manager.respondToRequest(request, CBATTErrorSuccess)
        }

        val firstRequest = requests.firstOrNull() ?: return
        val charUuid = firstRequest.characteristic.UUID.UUIDString
        val centralId = firstRequest.central.identifier.UUIDString
        val value = firstRequest.value?.toByteArray() ?: return

        scope.launch {
            val response = protocol.handleWrite(centralId, charUuid, value)
            if (response != null) {
                sendChunkedResponse(centralId, charUuid, response)
            }
        }
    }

    /**
     * Chunk [response] into [BleSegmentData] notifications and send each one
     * with flow control. Mirrors [AndroidBleGattServer.sendChunkedResponse].
     */
    private suspend fun sendChunkedResponse(centralId: String, charUuid: String, response: String) {
        val chunks = if (response.isEmpty()) listOf("") else response.chunked(notifyChunkSize)
        LogCat.d("[BLE] sendChunkedResponse central=$centralId charUuid=$charUuid responseLen=${response.length} chunks=${chunks.size}")

        for ((index, chunk) in chunks.withIndex()) {
            val segment = BleSegmentData.build(
                data = chunk,
                start = index == 0,
                end = index == chunks.lastIndex,
            )
            val payload = JsonHelper.jsonEncode(segment)
            val ok = sendNotificationBlocking(centralId, charUuid, payload)
            if (!ok) {
                LogCat.e("[BLE] sendChunkedResponse central=$centralId chunk $index/${chunks.size} failed, aborting")
                return
            }
        }
    }
}

private class PeripheralManagerDelegate(
    private val server: IosBleGattServer,
) : NSObject(), CBPeripheralManagerDelegateProtocol {

    override fun peripheralManagerDidUpdateState(peripheral: CBPeripheralManager) {
        LogCat.d("BLE peripheral manager state: ${peripheral.state}")
        if (peripheral.state == CBManagerStatePoweredOn) {
            server.onManagerReady()
        }
    }

    override fun peripheralManagerDidStartAdvertising(peripheral: CBPeripheralManager, error: NSError?) {
        if (error != null) {
            LogCat.e("BLE advertising failed: ${error.localizedDescription}")
        } else {
            LogCat.d("BLE advertising started successfully")
        }
    }

    override fun peripheralManager(
        peripheral: CBPeripheralManager,
        didAddService: CBService,
        error: NSError?,
    ) {
        server.onServiceAdded(error)
    }

    override fun peripheralManager(
        peripheral: CBPeripheralManager,
        didReceiveReadRequest: CBATTRequest,
    ) {
        server.onReadRequest(peripheral, didReceiveReadRequest)
    }

    override fun peripheralManager(
        peripheral: CBPeripheralManager,
        didReceiveWriteRequests: List<*>,
    ) {
        @Suppress("UNCHECKED_CAST")
        server.onWriteRequests(peripheral, didReceiveWriteRequests as List<CBATTRequest>)
    }

    override fun peripheralManagerIsReadyToUpdateSubscribers(peripheral: CBPeripheralManager) {
        server.onReadyToUpdateSubscribers()
    }
}
