package com.ismartcoding.plain.ble.client

import com.ismartcoding.plain.ble.BleUuids
import com.ismartcoding.plain.lib.toByteArray
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
import platform.Foundation.NSData
import platform.Foundation.NSNumber
import platform.darwin.NSObject
import kotlin.time.Duration.Companion.milliseconds

object IosBleScanner : BleScanner {

    private var centralManager: CBCentralManager? = null
    private val delegate = CentralDelegate()

    private val allDevices = mutableMapOf<String, IosBleGattClient>()
    private val pendingConnections = mutableMapOf<String, CompletableDeferred<Boolean>>()
    private var scanCallback: ((CBPeripheral, NSNumber, Map<Any?, *>) -> Unit)? = null
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

        scanCallback = { peripheral, rssi, advertisementData ->
            val (awareFlags, clientId) = parseServiceData(advertisementData, serviceUuid)
            // Match by clientId when advertised; fall back to peripheral UUID so
            // peers running older app versions (without clientId in serviceData)
            // still get a stable id.
            val key = clientId.ifEmpty { peripheral.identifier.UUIDString }
            val client = allDevices.getOrPut(key) {
                IosBleGattClient(peripheral, rssi.intValue, awareFlags, clientId)
            }
            client.rssi = rssi.intValue
            client.awareFlags = awareFlags
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

    /**
     * Extracts (awareFlags, clientId) from the CBAdvertisementDataServiceDataKey
     * entry for [serviceUuid]. Format mirrors AndroidBleGattServer: byte[0] =
     * flags, bytes[1..N] = clientId UTF-8 bytes. Returns (0, "") when absent.
     */
    private fun parseServiceData(
        advertisementData: Map<Any?, *>,
        serviceUuid: String,
    ): Pair<Int, String> {
        val serviceDataKey = platform.CoreBluetooth.CBAdvertisementDataServiceDataKey
            ?: return 0 to ""
        @Suppress("UNCHECKED_CAST")
        val serviceDataMap = advertisementData[serviceDataKey] as? Map<Any?, *>
            ?: return 0 to ""
        val targetUuid = CBUUID.UUIDWithString(serviceUuid)
        val nsData = serviceDataMap.entries.firstOrNull { (k, _) ->
            (k as? CBUUID)?.UUIDString.equals(targetUuid.UUIDString, ignoreCase = true)
        }?.value as? NSData ?: return 0 to ""
        val bytes = nsData.toByteArray()
        if (bytes.isEmpty()) return 0 to ""
        val flags = bytes[0].toInt() and 0xFF
        val clientId = if (bytes.size > 1) {
            try { bytes.decodeToString(1, bytes.size) } catch (_: Exception) { "" }
        } else ""
        return flags to clientId
    }

    override suspend fun findOne(id: String): BleGattClient? {
        if (!isReadyToUse()) return null
        // Fast path: a device with this clientId has already been discovered.
        allDevices[id]?.let { return it }
        return scan(BleUuids.SERVICE_UUID).firstOrNull { device ->
            device.id.equals(id, ignoreCase = true)
        }
    }

    /**
     * On iOS we can't construct a client from a clientId alone — the underlying
     * [CBPeripheral] must come from a scan result. Return an already-discovered
     * client if we have one; otherwise the caller must scan via [findOne].
     */
    override fun createClient(clientId: String): BleGattClient? = allDevices[clientId]

    suspend fun connectPeripheral(client: IosBleGattClient): Boolean {
        if (client.isConnected()) return true
        val manager = ensureCentralManager()
        val deferred = CompletableDeferred<Boolean>()
        // The peripheral UUID is the connection key on iOS — keep using it for
        // pendingConnections (which the CentralDelegate keys off of).
        val connectionKey = client.peripheral.identifier.UUIDString
        pendingConnections[connectionKey] = deferred
        manager.connectPeripheral(client.peripheral, null)
        val result = withTimeoutOrNull(10_000L.milliseconds) { deferred.await() }
        pendingConnections.remove(connectionKey)
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

    internal fun onPeripheralDiscovered(
        peripheral: CBPeripheral,
        rssi: NSNumber,
        advertisementData: Map<Any?, *>,
    ) {
        scanCallback?.invoke(peripheral, rssi, advertisementData)
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
        IosBleScanner.onPeripheralDiscovered(didDiscoverPeripheral, RSSI, advertisementData)
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
