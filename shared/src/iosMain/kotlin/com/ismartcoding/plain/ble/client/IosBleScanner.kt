package com.ismartcoding.plain.ble.client

import com.ismartcoding.plain.ble.BleServiceData
import com.ismartcoding.plain.ble.BleUuids
import com.ismartcoding.plain.lib.toByteArray
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.IosBluetoothMonitor
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
import platform.Foundation.NSLock
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

    // Scan lifecycle state, guarded by [scanLock]. The lock serializes the
    // scan flow's setup/teardown against pauseScan/resumeScan calls coming
    // from GATT users on other coroutines, so a resume can never restart the
    // radio after the flow has closed (and vice versa).
    private val scanLock = NSLock()
    private var scanServiceUuid: String? = null
    private var scanFlowActive = false
    private var pauseCount = 0

    private inline fun <T> withScanLock(block: () -> T): T {
        scanLock.lock()
        try {
            return block()
        } finally {
            scanLock.unlock()
        }
    }

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
        // Long enough for the user to answer the one-time system prompt.
        return withTimeoutOrNull(60_000L.milliseconds) { deferred.await() } ?: false
    }

    override fun scan(serviceUuid: String): Flow<BleGattClient> = callbackFlow {
        val manager = ensureCentralManager()
        if (manager.state != CBManagerStatePoweredOn) {
            LogCat.e("BLE scanner not ready, state=${manager.state}")
            close()
            return@callbackFlow
        }

        beginScan(serviceUuid) { peripheral, rssi, advertisementData ->
            val parts = parseServiceData(advertisementData, serviceUuid)
            // Match by shortId when advertised; fall back to peripheral UUID so
            // peers running older app versions (without serviceData) still get
            // a stable id.
            val key = parts?.shortId ?: peripheral.identifier.UUIDString
            val client = allDevices.getOrPut(key) {
                IosBleGattClient(
                    peripheral = peripheral,
                    rssi = rssi.intValue,
                    shortId = parts?.shortId ?: "",
                    awareSupported = parts?.awareSupported ?: false,
                    awareRunning = parts?.awareRunning ?: false,
                )
            }
            client.rssi = rssi.intValue
            trySend(client)
        }
        LogCat.d("BLE scan started for $serviceUuid")

        awaitClose {
            endScan()
            LogCat.d("BLE scan stopped")
        }
    }

    private fun beginScan(
        serviceUuid: String,
        onResult: (CBPeripheral, NSNumber, Map<Any?, *>) -> Unit,
    ) = withScanLock {
        scanCallback = onResult
        scanServiceUuid = serviceUuid
        scanFlowActive = true
        pauseCount = 0
        startRadio()
    }

    private fun endScan() = withScanLock {
        scanFlowActive = false
        ensureCentralManager().stopScan()
        isScanning = false
        scanCallback = null
    }

    private fun startRadio() {
        val uuid = scanServiceUuid ?: return
        ensureCentralManager().scanForPeripheralsWithServices(listOf(CBUUID.UUIDWithString(uuid)), null)
        isScanning = true
    }

    override fun pauseScan() = withScanLock {
        if (pauseCount++ == 0) {
            ensureCentralManager().stopScan()
            isScanning = false
        }
    }

    override fun resumeScan() = withScanLock {
        if (pauseCount > 0 && --pauseCount == 0 && scanFlowActive) {
            startRadio()
        }
    }

    override fun isScanPaused(): Boolean = withScanLock { pauseCount > 0 }

    /**
     * Parses the advertisement serviceData via [BleServiceData.decode].
     * Returns null when the serviceData is absent or too short.
     */
    private fun parseServiceData(
        advertisementData: Map<Any?, *>,
        serviceUuid: String,
    ): BleServiceData.Parts? {
        val serviceDataKey = platform.CoreBluetooth.CBAdvertisementDataServiceDataKey
            ?: return null
        @Suppress("UNCHECKED_CAST")
        val serviceDataMap = advertisementData[serviceDataKey] as? Map<Any?, *>
            ?: return null
        val targetUuid = CBUUID.UUIDWithString(serviceUuid)
        val nsData = serviceDataMap.entries.firstOrNull { (k, _) ->
            (k as? CBUUID)?.UUIDString.equals(targetUuid.UUIDString, ignoreCase = true)
        }?.value as? NSData ?: return null
        return BleServiceData.decode(nsData.toByteArray())
    }

    override suspend fun findOne(clientId: String): BleGattClient? {
        if (!isReadyToUse()) return null
        // Match by shortId (SHA256(clientId)[0:8] hex) — the scan-exposed
        // stable identifier. The full clientId is never broadcast.
        val shortId = BleServiceData.shortIdOf(clientId)
        allDevices[shortId]?.let { return it }
        return scan(BleUuids.SERVICE_UUID).firstOrNull { device ->
            device.id.equals(shortId, ignoreCase = true)
        }
    }

    /**
     * On iOS we can't construct a client from a clientId alone — the underlying
     * [CBPeripheral] must come from a scan result. Return an already-discovered
     * client if we have one; otherwise the caller must scan via [findOne].
     * Match by shortId.
     */
    override fun createClient(clientId: String): BleGattClient? {
        val shortId = BleServiceData.shortIdOf(clientId)
        return allDevices[shortId]
    }

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
        IosBluetoothMonitor.update(central.state)
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
