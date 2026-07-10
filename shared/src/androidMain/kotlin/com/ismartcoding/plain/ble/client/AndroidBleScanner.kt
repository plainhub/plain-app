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
import com.ismartcoding.plain.lib.isSPlus
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
                    trySend(addDevice(result.device, result.rssi))
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

    override suspend fun findOne(id: String): BleGattClient? {
        if (!isReadyToUse()) return null
        return scan(serviceUuid = BleUuids.SERVICE_UUID).firstOrNull { device ->
            id.equals(device.id, ignoreCase = true)
        }
    }

    @SuppressLint("MissingPermission")
    override fun createClient(mac: String): BleGattClient? {
        val device = getBluetoothAdapter().getRemoteDevice(mac) ?: return null
        val existing = allDevices.find { it.id.equals(mac, ignoreCase = true) }
        if (existing != null) return existing
        val client = AndroidBleGattClient(device, 0)
        allDevices.add(client)
        return client
    }

    @SuppressLint("MissingPermission")
    private fun addDevice(device: android.bluetooth.BluetoothDevice, rssi: Int): AndroidBleGattClient {
        val mac = device.address
        var d = allDevices.find { it.id == mac }
        if (d == null) {
            LogCat.v("Found device: ${device.name}, $mac, $rssi")
            d = AndroidBleGattClient(device, rssi)
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
