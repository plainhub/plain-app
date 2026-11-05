@file:OptIn(ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform.network.ble.server

import com.ismartcoding.plain.platform.network.ble.BleMessage
import com.ismartcoding.plain.platform.network.ble.RustBleWire
import com.ismartcoding.plain.platform.network.ble.BleUuids
import com.ismartcoding.plain.lib.toByteArray
import com.ismartcoding.plain.lib.toNSData
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
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
import platform.CoreBluetooth.CBMutableService
import platform.CoreBluetooth.CBPeripheralManager
import platform.CoreBluetooth.CBPeripheralManagerDelegateProtocol
import platform.CoreBluetooth.CBManagerStatePoweredOn
import platform.CoreBluetooth.CBUUID
import platform.CoreBluetooth.CBService
import platform.Foundation.NSError
import platform.darwin.NSObject
import kotlin.time.Duration.Companion.milliseconds
import com.ismartcoding.plain.platform.system.PlatformLock
import kotlinx.coroutines.sync.Mutex
import platform.CoreBluetooth.CBAdvertisementDataServiceUUIDsKey
import platform.CoreBluetooth.CBCharacteristic

class IosBleGattServer : BleGattServer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val protocol = BleServerProtocol()

    private var peripheralManager: CBPeripheralManager? = null
    private var delegate: PeripheralManagerDelegate? = null
    private val characteristics = mutableMapOf<String, CBMutableCharacteristic>()
    private var serviceAdded = false
    private var advertising = false
    private var servicePending = false
    private var pendingService: CBMutableService? = null
    private data class Advertising(val revision: Long = 0, val serviceUuid: String? = null, val closed: Boolean = false)
    private val desired = MutableStateFlow(Advertising())

    private val centralLock = PlatformLock()
    private val centrals = mutableMapOf<String, CBCentral>()
    private val sending = Mutex()

    private val notifyAckTimeoutMs = 10_000L

    override fun start(serviceUuid: String, revision: Long) {
        var next: Advertising
        while (true) {
            val previous = desired.value
            check(!previous.closed && revision > previous.revision) { "BLE advertiser closed or stale" }
            next = Advertising(revision, serviceUuid)
            if (desired.compareAndSet(previous, next)) break
        }
        val command = next
        scope.launch(Dispatchers.Main.immediate) {
            if (desired.value !== command) return@launch
            val manager = peripheralManager
            if (manager == null) {
                val del = PeripheralManagerDelegate(this@IosBleGattServer)
                delegate = del
                peripheralManager = CBPeripheralManager(del, null)
            } else if (serviceAdded && manager.state == CBManagerStatePoweredOn) {
                manager.stopAdvertising()
                advertising = false
                startAdvertising(manager)
            }
        }
    }

    override fun stop() {
        while (true) {
            val previous = desired.value
            if (previous.closed || desired.compareAndSet(previous, previous.copy(serviceUuid = null, closed = true))) break
        }
        protocol.clear()
        centralLock.withLock { centrals.clear() }
        pendingReadyDeferred.value?.complete(Unit)
        scope.launch(Dispatchers.Main.immediate) {
            val manager = peripheralManager
            peripheralManager = null
            delegate = null
            advertising = false
            serviceAdded = false
            servicePending = false
            pendingService = null
            characteristics.clear()
            manager?.stopAdvertising()
            manager?.removeAllServices()
        }
    }

    override fun sendNotification(mac: String, charUuid: String, value: ByteArray): Boolean {
        if (desired.value.closed) return false
        val requestId = protocol.nearbyRequestId(mac) ?: return false
        if (centralLock.withLock { centrals[mac] } == null) return false
        scope.launch(Dispatchers.Main.immediate) { sendChunkedResponse(mac, charUuid, BleMessage(requestId, RustBleWire.nearby(value))) }
        return true
    }

    override suspend fun sendNotificationBlocking(mac: String, charUuid: String, value: ByteArray): Boolean {
        if (desired.value.closed) return false
        val manager = peripheralManager ?: return false
        val char = characteristics[charUuid] ?: return false
        val central = centralLock.withLock { centrals[mac] } ?: return false
        val data = value.toNSData()

        // iOS `updateValue` returns false when the internal queue is full.
        // Wait for `peripheralManagerIsReadyToUpdateSubscribers` and retry.
        var attempts = 0
        while (attempts < 10) {
            if (desired.value.closed || peripheralManager !== manager) return false
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
        val service = CBMutableService(CBUUID.UUIDWithString(checkNotNull(desired.value.serviceUuid)), true)

        val charList = mutableListOf<CBMutableCharacteristic>()
        for (handler in protocol.handlers) {
            val charUuid = CBUUID.UUIDWithString(handler.charUuid)
            val char = CBMutableCharacteristic(
                charUuid,
                CBCharacteristicPropertyRead or CBCharacteristicPropertyWrite or CBCharacteristicPropertyNotify,
                null,
                CBAttributePermissionsReadable or CBAttributePermissionsWriteable,
            )
            characteristics[handler.charUuid] = char
            charList.add(char)
        }

        service.setCharacteristics(charList)
        pendingService = service
        manager.addService(service)
    }

    private fun startAdvertising(manager: CBPeripheralManager) {
        val command = desired.value
        if (command.closed || peripheralManager !== manager) return
        val serviceUuid = command.serviceUuid ?: return
        val uuid = CBUUID.UUIDWithString(serviceUuid)
        manager.startAdvertising(mapOf<Any?, Any?>(
            CBAdvertisementDataServiceUUIDsKey to listOf(uuid),
        ))
        advertising = true
    }

    internal fun onManagerReady(manager: CBPeripheralManager) {
        if (desired.value.closed || peripheralManager !== manager) return
        if (manager.state != CBManagerStatePoweredOn) {
            serviceAdded = false
            servicePending = false
            advertising = false
            pendingService = null
            characteristics.clear()
            return
        }
        if (!serviceAdded && !servicePending) {
            servicePending = true
            setupService(manager)
        }
    }

    internal fun onServiceAdded(manager: CBPeripheralManager, service: CBService, error: NSError?) {
        if (desired.value.closed || peripheralManager !== manager || pendingService !== service) return
        pendingService = null
        servicePending = false
        if (error != null) {
            LogCat.e("BLE GATT server add service failed: ${error.localizedDescription}")
            return
        }
        serviceAdded = true
        if (!advertising) startAdvertising(manager)
    }

    internal fun onAdvertisingStarted(manager: CBPeripheralManager, error: NSError?) {
        if (desired.value.closed || peripheralManager !== manager) {
            manager.stopAdvertising()
            return
        }
        if (error != null) LogCat.e("BLE advertising failed: ${error.localizedDescription}")
        else LogCat.d("BLE advertising started successfully")
    }

    internal fun onReadRequest(manager: CBPeripheralManager, request: CBATTRequest) {
        if (desired.value.closed || peripheralManager !== manager) return
        // Responses are now delivered via chunked notifications. Reply with
        // an empty value so legacy Read Requests still get a success status.
        request.value = ByteArray(0).toNSData()
        manager.respondToRequest(request, CBATTErrorSuccess)
    }

    internal fun onWriteRequests(manager: CBPeripheralManager, requests: List<CBATTRequest>) {
        if (desired.value.closed || peripheralManager !== manager) return
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
        server.onManagerReady(peripheral)
    }

    override fun peripheralManagerDidStartAdvertising(peripheral: CBPeripheralManager, error: NSError?) {
        server.onAdvertisingStarted(peripheral, error)
    }

    override fun peripheralManager(peripheral: CBPeripheralManager, central: CBCentral, didUnsubscribeFromCharacteristic: CBCharacteristic) {
        server.onUnsubscribe(central)
    }

    override fun peripheralManager(
        peripheral: CBPeripheralManager,
        didAddService: CBService,
        error: NSError?,
    ) {
        server.onServiceAdded(peripheral, didAddService, error)
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
