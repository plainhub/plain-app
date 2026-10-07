@file:OptIn(ExperimentalForeignApi::class)

package com.ismartcoding.plain.ble.server

import com.ismartcoding.plain.ble.BleMessage
import com.ismartcoding.plain.ble.RustBleWire
import com.ismartcoding.plain.discover.RustDiscoveryAdvertisement
import com.ismartcoding.plain.ble.BleUuids
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.toByteArray
import com.ismartcoding.plain.lib.toNSData
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import platform.CoreBluetooth.CBCentral
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
    private var advertisingJob: Job? = null

    private val centralLock = com.ismartcoding.plain.platform.PlatformLock()
    private val centrals = mutableMapOf<String, CBCentral>()
    private val sending = kotlinx.coroutines.sync.Mutex()

    private val notifyAckTimeoutMs = 10_000L

    override fun start() {
        scope.launch(Dispatchers.Main.immediate) {
            if (peripheralManager != null) return@launch
            val del = PeripheralManagerDelegate(this@IosBleGattServer)
            delegate = del
            peripheralManager = CBPeripheralManager(del, null)
        }
    }

    override fun stop() {
        protocol.clear()
        centralLock.withLock { centrals.clear() }
        scope.launch(Dispatchers.Main.immediate) {
            advertisingJob?.cancel()
            advertisingJob = null
            val manager = peripheralManager ?: return@launch
            manager.stopAdvertising()
            advertising = false
            if (serviceAdded) { manager.removeAllServices(); serviceAdded = false }
            peripheralManager = null
            delegate = null
            characteristics.clear()
        }
    }

    override fun refreshAdvertising() {
        scope.launch(Dispatchers.Main.immediate) {
            val manager = peripheralManager ?: return@launch
            manager.stopAdvertising()
            advertising = false
            startAdvertising(manager)
        }
    }

    override fun sendNotification(mac: String, charUuid: String, value: ByteArray): Boolean {
        val requestId = protocol.nearbyRequestId(mac) ?: return false
        if (centralLock.withLock { centrals[mac] } == null) return false
        scope.launch(Dispatchers.Main.immediate) { sendChunkedResponse(mac, charUuid, BleMessage(requestId, RustBleWire.nearby(value))) }
        return true
    }

    override suspend fun sendNotificationBlocking(mac: String, charUuid: String, value: ByteArray): Boolean {
        val manager = peripheralManager ?: return false
        val char = characteristics[charUuid] ?: return false
        val central = centralLock.withLock { centrals[mac] } ?: return false
        val data = value.toNSData()

        // iOS `updateValue` returns false when the internal queue is full.
        // Wait for `peripheralManagerIsReadyToUpdateSubscribers` and retry.
        var attempts = 0
        while (attempts < 10) {
            val deferred = CompletableDeferred<Unit>()
            // Stash the deferred so the delegate can complete it.
            pendingReadyDeferred.value = deferred
            val sent = manager.updateValue(data, forCharacteristic = char, onSubscribedCentrals = listOf(central))
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
        advertisingJob?.cancel()
        advertisingJob = scope.launch(Dispatchers.Main.immediate) {
            try {
                val payload = RustDiscoveryAdvertisement.ble()
                if (!isActive || peripheralManager !== manager) return@launch
                val uuid = CBUUID.UUIDWithString(BleUuids.SERVICE_UUID)
                val serviceDataMap: Map<Any?, Any?> = mapOf(uuid to payload.toNSData())
                manager.startAdvertising(mapOf<Any?, Any?>(
                    platform.CoreBluetooth.CBAdvertisementDataServiceUUIDsKey to listOf(uuid),
                    platform.CoreBluetooth.CBAdvertisementDataServiceDataKey to serviceDataMap,
                ))
                advertising = true
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { LogCat.e("BLE advertise error: ${error.message}") }
        }
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
            val charUuid = request.characteristic.UUID.UUIDString
            val centralId = request.central.identifier.UUIDString
            centralLock.withLock { centrals[centralId] = request.central }
            val value = request.value?.toByteArray() ?: continue
            scope.launch(Dispatchers.Main.immediate) {
                try {
                    val response = protocol.handleWrite(centralId, charUuid, value)
                    if (response != null) sendChunkedResponse(centralId, charUuid, response)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) { protocol.clearClient(centralId); LogCat.e("BLE exchange failed: ${error.message}") }
            }
        }
    }
    private suspend fun sendChunkedResponse(centralId: String, charUuid: String, response: BleMessage) = sending.withLock {
        val central = centralLock.withLock { centrals[centralId] } ?: return@withLock
        val limit = minOf(512, central.maximumUpdateValueLength.toInt())
        val encoder = RustBleWire.encoder(response.bytes, response.requestId, true, limit)
        try {
            while (centralLock.withLock { centrals[centralId] } === central) {
                val frame = encoder.next() ?: break
                check(sendNotificationBlocking(centralId, charUuid, frame)) { "BLE response notification failed" }
            }
        } finally { encoder.close() }
    }
    internal fun onUnsubscribe(central: CBCentral) {
        val id = central.identifier.UUIDString
        centralLock.withLock { centrals.remove(id) }
        protocol.clearClient(id)
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

    override fun peripheralManager(peripheral: CBPeripheralManager, central: CBCentral, didUnsubscribeFromCharacteristic: platform.CoreBluetooth.CBCharacteristic) {
        server.onUnsubscribe(central)
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
