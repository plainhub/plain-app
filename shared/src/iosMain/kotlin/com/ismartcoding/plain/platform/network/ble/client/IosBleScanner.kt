package com.ismartcoding.plain.platform.network.ble.client

import com.ismartcoding.plain.features.nearby.RustBleAdvertisement
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.system.IosBluetoothMonitor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerDelegateProtocol
import platform.CoreBluetooth.CBManagerStatePoweredOn
import platform.CoreBluetooth.CBPeripheral
import platform.CoreBluetooth.CBUUID
import platform.Foundation.NSError
import platform.Foundation.NSLock
import platform.Foundation.NSNumber
import platform.darwin.NSObject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.concurrent.Volatile
import platform.CoreBluetooth.CBAdvertisementDataOverflowServiceUUIDsKey
import platform.CoreBluetooth.CBAdvertisementDataServiceUUIDsKey

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

    @Volatile
    var isScanning = false
        private set

    private fun ensureCentralManager(): CBCentralManager {
        return centralManager ?: CBCentralManager(delegate, null).also { centralManager = it }
    }

    override fun isReadyToUse(): Boolean {
        return ensureCentralManager().state == CBManagerStatePoweredOn
    }

    override fun isAdvertiseReady(): Boolean = isReadyToUse()

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

    override fun scan(clientId: String?): Flow<BleGattClient> = callbackFlow<Triple<CBPeripheral, Int, List<String>>> {
        val manager = ensureCentralManager()
        if (manager.state != CBManagerStatePoweredOn) {
            LogCat.e("BLE scanner not ready, state=${manager.state}")
            close()
            return@callbackFlow
        }

        beginScan(clientId) { peripheral, rssi, advertisementData ->
            trySend(Triple(peripheral, rssi.intValue, serviceUuids(advertisementData)))
        }
        LogCat.d("BLE scan started for $clientId")

        awaitClose {
            endScan()
            LogCat.d("BLE scan stopped")
        }
    }.mapNotNull { (peripheral, rssi, uuids) ->
        val id = RustBleAdvertisement.decode(uuids) ?: return@mapNotNull null
        currentCoroutineContext().ensureActive()
        val existing = allDevices[id]
        val client = if (existing?.peripheral === peripheral) existing else {
            IosBleGattClient(peripheral = peripheral, rssi = rssi, id = id).also { allDevices[id] = it }
        }
        client.rssi = rssi
        client
    }.flowOn(Dispatchers.Main)

    private fun beginScan(
        clientId: String?,
        onResult: (CBPeripheral, NSNumber, Map<Any?, *>) -> Unit,
    ) = withScanLock {
        scanCallback = onResult
        scanServiceUuid = clientId
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
        val services = scanServiceUuid?.let { listOf(CBUUID.UUIDWithString(it)) }
        ensureCentralManager().scanForPeripheralsWithServices(services, null)
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

    private fun serviceUuids(advertisementData: Map<Any?, *>): List<String> =
        listOf(CBAdvertisementDataServiceUUIDsKey, CBAdvertisementDataOverflowServiceUUIDsKey)
            .flatMap { (advertisementData[it] as? List<*>).orEmpty() }
            .mapNotNull { (it as? CBUUID)?.UUIDString }

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
        allDevices.values.filter { it.peripheral.identifier.UUIDString == id }.forEach { it.onDisconnected() }
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
