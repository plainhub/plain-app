package com.ismartcoding.plain.ble.client

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
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.ble.BleServiceData
import com.ismartcoding.plain.ble.BleUuids
import com.ismartcoding.plain.lib.extensions.hasPermission
import com.ismartcoding.plain.platform.isSPlus
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.channels.awaitClose
import java.util.UUID

object AndroidBleScanner : BleScanner {

    private val allDevices = mutableListOf<AndroidBleGattClient>()
    private var scanCallback: ScanCallback? = null

    @Volatile
    var isScanning = false
        private set

    private val cachedNames = mutableMapOf<String, String>()

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

    override suspend fun ensurePermission(): Boolean {
        return isBlePermissionGranted()
    }

    @SuppressLint("MissingPermission")
    override fun scan(serviceUuid: String): Flow<BleGattClient> {
        return callbackFlow {
            LogCat.d("Scan bluetooth devices for $serviceUuid")

            scanCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    val parts = parseServiceData(result, serviceUuid)
                    trySend(addDevice(result.device, result.rssi, parts))
                }
            }
            val filterUuid = ParcelUuid.fromString(serviceUuid)
            val filters = arrayListOf(ScanFilter.Builder().setServiceUuid(filterUuid).build())
            getBluetoothAdapter().bluetoothLeScanner?.startScan(filters, ScanSettings.Builder().build(), scanCallback)
            isScanning = true

            awaitClose {
                stopScan()
            }
        }
    }

    /**
     * Parses the scan response serviceData via [BleServiceData.decode].
     * Returns null when the serviceData is absent (peer is not advertising
     * yet) — in that case the device falls back to being identified by MAC.
     */
    private fun parseServiceData(result: ScanResult, serviceUuid: String): BleServiceData.Parts? {
        val data = result.scanRecord?.serviceData?.get(ParcelUuid.fromString(serviceUuid)) ?: return null
        return BleServiceData.decode(data)
    }

    override suspend fun findOne(clientId: String): BleGattClient? {
        if (!isReadyToUse()) return null
        // Match by shortId (SHA256(clientId)[0:8] hex) — the scan-exposed
        // stable identifier. The full clientId is never broadcast.
        val shortId = BleServiceData.shortIdOf(clientId)
        allDevices.find { it.id.equals(shortId, ignoreCase = true) }?.let { return it }
        return scan(serviceUuid = BleUuids.SERVICE_UUID).firstOrNull { device ->
            shortId.equals(device.id, ignoreCase = true)
        }
    }

    @SuppressLint("MissingPermission")
    override fun createClient(clientId: String): BleGattClient? {
        // On Android we can't construct a BleGattClient from a clientId alone —
        // the underlying BluetoothDevice (with its current MAC) must come from
        // a scan result. Return an already-discovered client if we have one;
        // otherwise the caller must scan via [findOne]. Match by shortId.
        val shortId = BleServiceData.shortIdOf(clientId)
        return allDevices.find { it.id.equals(shortId, ignoreCase = true) }
    }

    @SuppressLint("MissingPermission")
    private fun addDevice(
        device: android.bluetooth.BluetoothDevice,
        rssi: Int,
        parts: BleServiceData.Parts?,
    ): AndroidBleGattClient {
        // Match by shortId (the stable peer match key parsed from serviceData).
        // Falls back to the BLE MAC only when the peer hasn't started
        // advertising yet (parts is null).
        val key = parts?.shortId ?: device.address
        var d = allDevices.find { it.id == key }
        if (d == null) {
            LogCat.v("Found device: ${device.name}, shortId=${parts?.shortId}, mac=${device.address}, $rssi")
            d = AndroidBleGattClient(
                device = device,
                rssi = rssi,
                shortId = parts?.shortId ?: "",
                awareSupported = parts?.awareSupported ?: false,
                awareRunning = parts?.awareRunning ?: false,
            )
            allDevices.add(d)
        } else {
            d.rssi = rssi
        }
        return d
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (isScanning) {
            getBluetoothAdapter().bluetoothLeScanner?.stopScan(scanCallback)
            isScanning = false
        }
    }

    fun stopScanAndRelease() {
        stopScan()
        disconnectAll()
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

    private fun disconnectAll() {
        if (allDevices.isEmpty()) return
        val connected = allDevices.filter { it.isConnected() }.toList()
        LogCat.d("Disconnecting bluetooth: ${connected.joinToString(", ") { it.id }}")
        for (device in connected) {
            device.disconnect()
        }
        allDevices.clear()
    }
}
