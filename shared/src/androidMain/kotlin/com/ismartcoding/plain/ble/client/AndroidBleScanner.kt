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
                    val awareFlags = parseAwareFlags(result, serviceUuid)
                    val clientId = parseClientId(result, serviceUuid)
                    trySend(addDevice(result.device, result.rssi, clientId, awareFlags))
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
     * Extracts the 1-byte Aware flags from the scan response serviceData.
     * Returns 0 when the serviceData is absent (e.g. peer is not advertising yet).
     */
    private fun parseAwareFlags(result: ScanResult, serviceUuid: String): Int {
        val data = result.scanRecord?.serviceData?.get(ParcelUuid.fromString(serviceUuid)) ?: return 0
        if (data.isEmpty()) return 0
        return data[0].toInt() and 0xFF
    }

    /**
     * Extracts the peer's clientId (TempData.clientId, a 13-char short UUID)
     * from the scan response serviceData. Format: byte[0] = aware flags,
     * byte[1..N] = clientId UTF-8 bytes. Returns "" when the serviceData is
     * absent (peer is not advertising yet) — in that case the device falls
     * back to being identified by MAC until a proper clientId is seen.
     */
    private fun parseClientId(result: ScanResult, serviceUuid: String): String {
        val data = result.scanRecord?.serviceData?.get(ParcelUuid.fromString(serviceUuid)) ?: return ""
        if (data.size <= 1) return ""
        return try {
            String(data, 1, data.size - 1, Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    override suspend fun findOne(id: String): BleGattClient? {
        if (!isReadyToUse()) return null
        // Fast path: a device with this clientId has already been discovered.
        allDevices.find { it.id == id }?.let { return it }
        return scan(serviceUuid = BleUuids.SERVICE_UUID).firstOrNull { device ->
            id.equals(device.id, ignoreCase = true)
        }
    }

    @SuppressLint("MissingPermission")
    override fun createClient(clientId: String): BleGattClient? {
        // On Android we can't construct a BleGattClient from a clientId alone —
        // the underlying BluetoothDevice (with its current MAC) must come from
        // a scan result. Return an already-discovered client if we have one;
        // otherwise the caller must scan via [findOne].
        return allDevices.find { it.id == clientId }
    }

    @SuppressLint("MissingPermission")
    private fun addDevice(
        device: android.bluetooth.BluetoothDevice,
        rssi: Int,
        clientId: String,
        awareFlags: Int = 0,
    ): AndroidBleGattClient {
        // Match by clientId (the stable peer id parsed from serviceData).
        // Falls back to the BLE MAC only when the peer hasn't started
        // advertising its clientId yet (clientId is empty).
        val key = clientId.ifEmpty { device.address }
        var d = allDevices.find { it.id == key }
        if (d == null) {
            LogCat.v("Found device: ${device.name}, clientId=$clientId, mac=${device.address}, $rssi, awareFlags=$awareFlags")
            d = AndroidBleGattClient(device, rssi, awareFlags, clientId)
            allDevices.add(d)
        } else {
            d.rssi = rssi
            d.awareFlags = awareFlags
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
