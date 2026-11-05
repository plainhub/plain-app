package com.ismartcoding.plain.platform.network.ble.client

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import java.util.concurrent.CopyOnWriteArrayList
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.platform.network.ble.BleUuids
import com.ismartcoding.plain.features.nearby.RustBleAdvertisement
import com.ismartcoding.plain.features.bluetooth.client.BluetoothPermission
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.mapNotNull
import com.ismartcoding.plain.lib.extensions.hasPermission
import com.ismartcoding.plain.platform.isSPlus
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import android.bluetooth.BluetoothDevice

object AndroidBleScanner : BleScanner {

    // Mutated from BLE scan callbacks, which the system can invoke on different
    // background threads concurrently, so a plain mutableListOf (ArrayList) is
    // not safe here.
    private val allDevices = CopyOnWriteArrayList<AndroidBleGattClient>()

    // Scan lifecycle state, guarded by the scanner monitor (@Synchronized
    // methods). The lock serializes the scan flow's setup/teardown against
    // pauseScan/resumeScan calls coming from GATT users on other coroutines,
    // so a resume can never restart the radio after the flow has closed
    // (and vice versa).
    private var scanCallback: ScanCallback? = null
    private var scanFilter: List<ScanFilter>? = null
    private var scanSettings: ScanSettings? = null
    private var scanFlowActive = false
    private var pauseCount = 0

    @Volatile
    var isScanning = false
        private set

    fun getBluetoothAdapter(): BluetoothAdapter {
        val manager = appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return manager.adapter
    }

    override fun isReadyToUse(): Boolean {
        if (isSPlus()) {
            return appContext.hasPermission(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        }
        return getBluetoothAdapter().isEnabled && appContext.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    override fun isAdvertiseReady(): Boolean {
        if (!isReadyToUse()) return false
        if (isSPlus() && !appContext.hasPermission(Manifest.permission.BLUETOOTH_ADVERTISE)) return false
        return true
    }

    fun isBlePermissionGranted(): Boolean {
        if (isSPlus()) {
            return appContext.hasPermission(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
            )
        }
        return appContext.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    override suspend fun ensurePermission(): Boolean = BluetoothPermission.ensurePermissionAsync()

    @SuppressLint("MissingPermission")
    override fun scan(clientId: String?): Flow<BleGattClient> {
        return callbackFlow<ScanResult> {
            LogCat.d("Scan bluetooth devices for $clientId")
            beginScan(clientId) { result ->
                trySend(result)
            }

            awaitClose {
                endScan()
            }
        }.mapNotNull { result ->
            val uuids = result.scanRecord?.serviceUuids.orEmpty().map { it.toString() }
            val id = RustBleAdvertisement.decode(uuids) ?: return@mapNotNull null
            currentCoroutineContext().ensureActive()
            addDevice(result.device, result.rssi, id)
        }
    }

    @SuppressLint("MissingPermission")
    @Synchronized
    private fun beginScan(clientId: String?, onResult: (ScanResult) -> Unit) {
        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                onResult(result)
            }
        }
        val filter = ScanFilter.Builder()
        if (clientId == null) {
            filter.setServiceUuid(ParcelUuid.fromString(BleUuids.DISCOVERY_UUID), ParcelUuid.fromString(BleUuids.DISCOVERY_UUID_MASK))
        } else {
            filter.setServiceUuid(ParcelUuid.fromString(clientId))
        }
        scanFilter = listOf(filter.build())
        scanSettings = ScanSettings.Builder().build()
        scanFlowActive = true
        pauseCount = 0
        startRadio()
    }

    @Synchronized
    private fun endScan() {
        scanFlowActive = false
        stopRadio()
    }

    @SuppressLint("MissingPermission")
    private fun startRadio() {
        val callback = scanCallback ?: return
        val filters = scanFilter ?: return
        getBluetoothAdapter().bluetoothLeScanner?.startScan(filters, scanSettings, callback)
        isScanning = true
    }

    @SuppressLint("MissingPermission")
    private fun stopRadio() {
        if (isScanning) {
            getBluetoothAdapter().bluetoothLeScanner?.stopScan(scanCallback)
            isScanning = false
        }
    }

    @Synchronized
    override fun pauseScan() {
        if (pauseCount++ == 0) {
            stopRadio()
        }
    }

    @Synchronized
    override fun resumeScan() {
        if (pauseCount > 0 && --pauseCount == 0 && scanFlowActive) {
            startRadio()
        }
    }

    @Synchronized
    override fun isScanPaused(): Boolean = pauseCount > 0



    override fun createClient(clientId: String): BleGattClient? = allDevices.find { it.id == clientId }

    @SuppressLint("MissingPermission")
    private fun addDevice(device: BluetoothDevice, rssi: Int, clientId: String): AndroidBleGattClient {
        val current = allDevices.find { it.id == clientId && it.device.address == device.address }
        if (current != null) {
            current.rssi = rssi
            return current
        }
        allDevices.removeAll { it.id == clientId }
        return AndroidBleGattClient(
            device = device,
            context = appContext,
            logger = { level, message ->
                when (level) {
                    "D" -> LogCat.d(message)
                    "W" -> LogCat.w(message)
                    else -> LogCat.e(message)
                }
            },
            onDisconnected = ::teardown,
            rssi = rssi,
            id = clientId,
        ).also { allDevices.add(it) }
    }

    override fun teardownConnection(device: BleGattClient) {
        if (device is AndroidBleGattClient && device.isConnected()) {
            device.disconnect()
        }
    }

    fun teardown(device: AndroidBleGattClient) {
        if (device.isConnected()) {
            device.disconnect()
        }
    }
}
