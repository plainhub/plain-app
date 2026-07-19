@file:OptIn(ExperimentalForeignApi::class)

package com.ismartcoding.plain.ble.server

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.ble.BleUuids
import com.ismartcoding.plain.lib.toByteArray
import com.ismartcoding.plain.lib.toNSData
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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

private const val READY_SIGNAL = "1"
// iOS Wi-Fi Aware is stubbed (not yet implemented) — always advertise flags = 0.
// The clientId portion of serviceData is still populated so Android peers can
// identify the iOS device when BLE MAC randomization rotates.
private const val FLAG_AWARE_SUPPORTED: Byte = 0
private const val FLAG_AWARE_RUNNING: Byte = 0
private const val CLIENT_ID_MAX_LEN = 20

class IosBleGattServer : BleGattServer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val protocol = BleServerProtocol()

    private var peripheralManager: CBPeripheralManager? = null
    private var delegate: PeripheralManagerDelegate? = null
    private val characteristics = mutableMapOf<String, CBMutableCharacteristic>()
    private var serviceAdded = false
    private var advertising = false

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
        // Build the serviceData payload (flags byte + clientId UTF-8 bytes) and
        // attach it to the advertisement so peers can identify this device by
        // its stable clientId instead of the BLE MAC (which rotates).
        val clientIdBytes = TempData.clientId.encodeToByteArray()
        val clientIdLen = minOf(clientIdBytes.size, CLIENT_ID_MAX_LEN)
        val payload = ByteArray(1 + clientIdLen)
        payload[0] = (FLAG_AWARE_SUPPORTED.toInt() or FLAG_AWARE_RUNNING.toInt()).toByte()
        if (clientIdLen > 0) {
            clientIdBytes.copyInto(payload, 1, 0, clientIdLen)
        }
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
        val charUuid = request.characteristic.UUID.UUIDString
        val centralId = request.central.identifier.UUIDString
        val offset = request.offset.toInt()

        val payload = protocol.handleRead(centralId, charUuid, offset)
        request.value = payload.toNSData()
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
            val processed = protocol.handleWrite(centralId, charUuid, value)
            if (processed) {
                notifyReady(manager, centralId, charUuid)
            }
        }
    }

    private fun notifyReady(manager: CBPeripheralManager, centralId: String, charUuid: String) {
        val char = characteristics[charUuid] ?: return
        val readyData = READY_SIGNAL.encodeToByteArray().toNSData()
        manager.updateValue(readyData, forCharacteristic = char, onSubscribedCentrals = null)
        LogCat.d("BLE GATT server sent ready notification to $centralId")
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
}
