package com.ismartcoding.plain.ble.client

import com.ismartcoding.plain.ble.BleUuids
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerDelegateProtocol
import platform.CoreBluetooth.CBManagerStatePoweredOn
import platform.CoreBluetooth.CBPeripheral
import platform.CoreBluetooth.CBUUID
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import platform.darwin.NSObject
import kotlin.time.Duration.Companion.milliseconds

object IosBleScanner : BleScanner {

    private var centralManager: CBCentralManager? = null
    private val delegate = CentralDelegate()

    private val allDevices = mutableMapOf<String, IosBleGattClient>()
    private val pendingConnections = mutableMapOf<String, CompletableDeferred<Boolean>>()
    private var scanCallback: ((CBPeripheral, NSNumber) -> Unit)? = null
    private var stateChangeCallback: (() -> Unit)? = null

    @kotlin.concurrent.Volatile
    var isScanning = false
        private set

    private fun ensureCentralManager(): CBCentralManager {
        return centralManager ?: CBCentralManager(delegate, null).also { centralManager = it }
    }

    override fun isReadyToUse(): Boolean {
        return ensureCentralManager().state == CBManagerStatePoweredOn
    }

    override fun isAdvertiseReady(): Boolean = isReadyToUse()

    fun isBlePermissionGranted(): Boolean = isReadyToUse()

    override suspend fun ensurePermission(): Boolean {
        val manager = ensureCentralManager()
        if (manager.state == CBManagerStatePoweredOn) return true
        val deferred = CompletableDeferred<Boolean>()
        stateChangeCallback = {
            val state = manager.state
            if (state == CBManagerStatePoweredOn) {
                deferred.complete(true)
            } else if (state != 0L && state != 1L) {
                deferred.complete(false)
            }
        }
        return withTimeoutOrNull(10_000L.milliseconds) { deferred.await() } ?: false
    }

    override fun scan(serviceUuid: String): Flow<BleGattClient> = callbackFlow {
        val manager = ensureCentralManager()
        if (manager.state != CBManagerStatePoweredOn) {
            LogCat.e("BLE scanner not ready, state=${manager.state}")
            close()
            return@callbackFlow
        }

        scanCallback = { peripheral, rssi ->
            val id = peripheral.identifier.UUIDString
            val client = allDevices.getOrPut(id) { IosBleGattClient(peripheral, 0) }
            client.rssi = rssi.intValue
            trySend(client)
        }

        val services = listOf(CBUUID.UUIDWithString(serviceUuid))
        manager.scanForPeripheralsWithServices(services, null)
        isScanning = true
        LogCat.d("BLE scan started for $serviceUuid")

        awaitClose {
            manager.stopScan()
            isScanning = false
            scanCallback = null
            LogCat.d("BLE scan stopped")
        }
    }

    override suspend fun findOne(id: String): BleGattClient? {
        if (!isReadyToUse()) return null
        return scan(BleUuids.SERVICE_UUID).firstOrNull { device ->
            device.id.equals(id, ignoreCase = true)
        }
    }

    override fun createClient(mac: String): BleGattClient? = null

    suspend fun connectPeripheral(client: IosBleGattClient): Boolean {
        if (client.isConnected()) return true
        val manager = ensureCentralManager()
        val deferred = CompletableDeferred<Boolean>()
        pendingConnections[client.id] = deferred
        manager.connectPeripheral(client.peripheral, null)
        val result = withTimeoutOrNull(10_000L.milliseconds) { deferred.await() }
        pendingConnections.remove(client.id)
        return result == true
    }

    override fun teardownConnection(device: BleGattClient) {
        if (device is IosBleGattClient) {
            ensureCentralManager().cancelPeripheralConnection(device.peripheral)
        }
    }

    fun stopScan() {
        centralManager?.stopScan()
        isScanning = false
    }

    internal fun onStateChanged() {
        stateChangeCallback?.invoke()
    }

    internal fun onPeripheralDiscovered(peripheral: CBPeripheral, rssi: NSNumber) {
        scanCallback?.invoke(peripheral, rssi)
    }

    internal fun onPeripheralConnected(peripheral: CBPeripheral) {
        val id = peripheral.identifier.UUIDString
        pendingConnections[id]?.complete(true)
    }

    internal fun onPeripheralDisconnected(peripheral: CBPeripheral) {
        val id = peripheral.identifier.UUIDString
        pendingConnections[id]?.complete(false)
    }
}

private class CentralDelegate : NSObject(), CBCentralManagerDelegateProtocol {
    override fun centralManagerDidUpdateState(central: CBCentralManager) {
        LogCat.d("BLE central manager state: ${central.state}")
        IosBleScanner.onStateChanged()
    }

    override fun centralManager(
        central: CBCentralManager,
        didDiscoverPeripheral: CBPeripheral,
        advertisementData: Map<Any?, *>,
        RSSI: NSNumber,
    ) {
        IosBleScanner.onPeripheralDiscovered(didDiscoverPeripheral, RSSI)
    }

    override fun centralManager(
        central: CBCentralManager,
        didConnectPeripheral: CBPeripheral,
    ) {
        LogCat.d("BLE peripheral connected: ${didConnectPeripheral.identifier.UUIDString}")
        IosBleScanner.onPeripheralConnected(didConnectPeripheral)
    }

    override fun centralManager(
        central: CBCentralManager,
        didDisconnectPeripheral: CBPeripheral,
        error: NSError?,
    ) {
        LogCat.d("BLE peripheral disconnected: ${didDisconnectPeripheral.identifier.UUIDString}")
        IosBleScanner.onPeripheralDisconnected(didDisconnectPeripheral)
    }
}
