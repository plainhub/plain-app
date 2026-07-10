package com.ismartcoding.plain.ble.server

import com.ismartcoding.plain.ble.BleUuids
import com.ismartcoding.plain.lib.logcat.LogCat
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
import platform.CoreBluetooth.CBPeripheralManagerDelegate
import platform.CoreBluetooth.CBManagerStatePoweredOn
import platform.CoreBluetooth.CBUUID
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSNumber

private const val READY_SIGNAL = "1"
private const val FLAG_AWARE_SUPPORTED: Byte = 0
private const val FLAG_AWARE_RUNNING: Byte = 0

class IosBleGattServer : BleGattServer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
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
            for (handler in handlers) {
                characteristics[handler.charUuid]?.let { char ->
                    manager.removeCharacteristic(char)
                }
            }
            val serviceUuid = CBUUID.UUIDWithString(BleUuids.SERVICE_UUID)
            val service = manager.services?.firstOrNull { it.UUID == serviceUuid }
            service?.let { manager.removeService(it) }
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

        for (handler in handlers) {
            val charUuid = CBUUID.UUIDWithString(handler.charUuid)
            val char = CBMutableCharacteristic(
                charUuid,
                CBCharacteristicPropertyRead or CBCharacteristicPropertyWrite or CBCharacteristicPropertyNotify,
                null,
                CBAttributePermissionsReadable or CBAttributePermissionsWriteable,
            )
            val cccDescriptor = CBMutableDescriptor(
                CBUUID.UUIDWithString(BleUuids.CCC_DESCRIPTOR_UUID),
                null,
            )
            char.descriptors = listOf(cccDescriptor)
            characteristics[handler.charUuid] = char
        }

        service.characteristics = characteristics.values.toList()
        manager.addService(service)
    }

    private fun startAdvertising(manager: CBPeripheralManager) {
        val advertisingData = mapOf<Any?, Any?>(
            platform.CoreBluetooth.CBAdvertisementDataServiceUUIDsKey to
                listOf(CBUUID.UUIDWithString(BleUuids.SERVICE_UUID)),
        )
        manager.startAdvertising(advertisingData)
        advertising = true
        LogCat.d("BLE GATT server advertising started")
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
) : CBPeripheralManagerDelegate() {

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

    override fun peripheralManagerDidAddService(
        peripheral: CBPeripheralManager,
        service: CBMutableService,
        error: NSError?,
    ) {
        server.onServiceAdded(error)
    }

    override fun peripheralManagerDidReceiveReadRequest(
        peripheral: CBPeripheralManager,
        request: CBATTRequest,
    ) {
        server.onReadRequest(peripheral, request)
    }

    override fun peripheralManagerDidReceiveWriteRequests(
        peripheral: CBPeripheralManager,
        requests: List<*>,
    ) {
        @Suppress("UNCHECKED_CAST")
        server.onWriteRequests(peripheral, requests as List<CBATTRequest>)
    }
}

private fun ByteArray.toNSData(): NSData = NSData.dataWithBytes(this, this.size.toULong())

private fun NSData.toByteArray(): ByteArray = ByteArray(this.length.toInt()).also { buf ->
    this.getBytes(buf)
}
