package com.ismartcoding.plain.features.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.location.LocationManager
import android.os.Build
import android.os.ParcelUuid
import androidx.annotation.RequiresApi
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.hasPermission
import com.ismartcoding.lib.isSPlus
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.MainApp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

object BluetoothUtil {
    private val allDevices = ArrayList<BTDevice>()
    var currentBTDevice: BTDevice? = null
    private var scanCallback: ScanCallback? = null
    private val operationQueue = ConcurrentLinkedQueue<IBTOperation>()
    var canContinue = false
    var pendingOperation: IBTOperation? = null
        private set
    var isScanning = false

    private const val GATT_MIN_MTU_SIZE = 23
    private const val GATT_MAX_MTU_SIZE = 517

    fun isBluetoothReadyToUseWithPermissionRequest(): Boolean {
        if (isSPlus()) {
            if (!isScanConnectGranted(MainApp.instance)) {
                requestScanConnectBluetooth()
                return false
            }
        } else {
            if (!isBluetoothEnabled()) {
                requestEnableBluetooth()
                return false
            }

            if (!isLocationEnabled(MainApp.instance)) {
                requestLocationPermission()
                return false
            }

            if (shouldEnableGPS()) {
                sendEvent(RequestBluetoothLocationGPSPermissionEvent())
                return false
            }
        }

        return true
    }

    private fun shouldEnableGPS(): Boolean {
        val locationManager = MainApp.instance.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val hasGPS =
            if (isSPlus()) {
                locationManager.getProviderProperties(LocationManager.GPS_PROVIDER) != null
            } else {
                @Suppress("DEPRECATION")
                locationManager.getProvider(LocationManager.GPS_PROVIDER) != null
            }

        return hasGPS && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    suspend fun ensurePermissionAsync(): Boolean {
        val ready = isBluetoothReadyToUseWithPermissionRequest()
        if (!ready) {
            canContinue = false
            while (true) {
                LogCat.d("waiting for bluetooth permission accepted or denied")
                if (canContinue) {
                    return isBluetoothReadyToUse()
                }
                delay(500)
            }
        }

        sendEvent(BluetoothPermissionResultEvent())
        return true
    }

    fun isBluetoothReadyToUse(): Boolean {
        if (isSPlus()) {
            if (!isScanConnectGranted(MainApp.instance)) {
                return false
            }
        } else {
            if (!isBluetoothEnabled()) {
                return false
            }

            if (!isLocationEnabled(MainApp.instance)) {
                return false
            }

            if (shouldEnableGPS()) {
                return false
            }
        }

        return true
    }

    suspend fun getCurrentBTDeviceAsync(mac: String): BTDevice? {
        if (currentBTDevice != null) {
            return currentBTDevice
        }

        sendEvent(BluetoothFindOneEvent(mac))
        return withTimeoutOrNull(2000) {
            while (true) {
                if (currentBTDevice != null) {
                    break
                }
                delay(200)
            }
            return@withTimeoutOrNull currentBTDevice
        }
    }

    suspend fun findOneAsync(mac: String): BTDevice? {
        if (!isBluetoothReadyToUse()) {
            return null
        }

        return scan().firstOrNull { device ->
            return@firstOrNull mac.equals(device.mac, true)
        }
    }

    @SuppressLint("MissingPermission")
    fun scan(): Flow<BTDevice> {
        return callbackFlow {
            LogCat.d("Scan bluetooth devices")

            scanCallback =
                object : ScanCallback() {
                    override fun onScanResult(
                        callbackType: Int,
                        result: ScanResult,
                    ) {
                        trySend(addBTDevice(result.device, result.rssi))
                    }
                }
            val filters = arrayListOf(ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(BTDevice.SERVICE_UUID.toString())).build())
            getBluetoothAdapter().bluetoothLeScanner.startScan(filters, ScanSettings.Builder().build(), scanCallback)
            isScanning = true

            awaitClose {
                stopScan()
            }
        }
    }

    private fun getBluetoothAdapter(): BluetoothAdapter {
        val bluetoothManager = MainApp.instance.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.adapter
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
        operationQueue.clear()
        disconnectAll()
    }

    fun connect(device: BTDevice) {
        if (device.isConnected()) {
            LogCat.e("Already connected to ${device.mac}!")
        } else {
            enqueueOperation(BTOperationConnect(device))
        }
    }

    fun requestMtu(
        device: BTDevice,
        mtu: Int,
    ) {
        if (device.isConnected()) {
            enqueueOperation(BTOperationMtuRequest(device, mtu.coerceIn(GATT_MIN_MTU_SIZE, GATT_MAX_MTU_SIZE)))
        } else {
            LogCat.e("Not connected to ${device.mac}, cannot request MTU update!")
        }
    }

    fun teardownConnection(device: BTDevice) {
        if (device.isConnected()) {
            enqueueOperation(BTOperationDisconnect(device))
        } else {
            LogCat.e("Not connected to ${device.mac}, cannot teardown connection!")
        }
    }

    @Synchronized
    fun signalEndOfOperation() {
        LogCat.d("End of $pendingOperation")
        pendingOperation = null
        if (operationQueue.isNotEmpty()) {
            doNextOperation()
        }
    }

    @Synchronized
    fun enqueueOperation(operation: IBTOperation) {
        operationQueue.add(operation)
        if (pendingOperation == null) {
            doNextOperation()
        }
    }

    @Synchronized
    private fun doNextOperation() {
        if (pendingOperation != null) {
            LogCat.e("doNextOperation() called when an operation is pending! Aborting.")
            return
        }

        val operation =
            operationQueue.poll() ?: run {
                LogCat.d("Operation queue empty, returning")
                return
            }
        pendingOperation = operation

        // Handle Connect separately from other operations that require device to be connected
        if (operation is BTOperationConnect) {
            operation.run()
            return
        }

        if (!operation.device.isConnected()) {
            LogCat.e("Not connected to ${operation.device.device.address}! Aborting $operation operation.")
            signalEndOfOperation()
            return
        }

        operation.run()
    }

    @SuppressLint("MissingPermission")
    private fun addBTDevice(
        device: BluetoothDevice,
        rssi: Int,
    ): BTDevice {
        var d = allDevices.find { it.device.address == device.address }
        LogCat.v("Found device: ${device.name}, ${device.address}, $rssi")
        if (d == null) {
            d = BTDevice(device, rssi)
            allDevices.add(d)
        } else {
            d.rssi = rssi
        }
        return d
    }

    private fun isBluetoothEnabled(): Boolean {
        return getBluetoothAdapter().isEnabled
    }

    private fun isLocationEnabled(context: Context): Boolean {
        return context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun isScanConnectGranted(context: Context): Boolean {
        return context.hasPermission(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    }

    private fun requestLocationPermission() {
        sendEvent(RequestBluetoothLocationPermissionEvent())
    }

    private fun requestEnableBluetooth() {
        sendEvent(RequestEnableBluetoothEvent())
    }

    private fun requestScanConnectBluetooth() {
        sendEvent(RequestScanConnectBluetoothEvent())
    }

    private fun disconnectAll() {
        if (allDevices.isEmpty()) {
            return
        }
        val connectedDevices = allDevices.filter { it.isConnected() }.toList()
        LogCat.d("Disconnecting bluetooth: ${connectedDevices.joinToString(", ") { it.mac }}")
        for (device in connectedDevices) {
            teardownConnection(device)
        }
        allDevices.clear()
    }
}
