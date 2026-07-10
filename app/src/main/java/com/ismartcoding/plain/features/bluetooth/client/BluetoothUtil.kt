package com.ismartcoding.plain.features.bluetooth.client

import android.Manifest
import android.content.Context
import android.location.LocationManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.ismartcoding.plain.ble.client.AndroidBleScanner
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.lib.extensions.hasPermission
import com.ismartcoding.plain.lib.isSPlus
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.MainApp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

object BluetoothUtil {
    var canContinue = false

    fun isBluetoothReadyToUseWithPermissionRequest(): Boolean {
        if (isSPlus()) {
            if (!isScanConnectGranted(MainApp.instance)) {
                requestScanConnectBluetooth()
                return false
            }
            if (!isAdvertiseGranted(MainApp.instance)) {
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
                delay(500.milliseconds)
            }
        }

        sendEvent(BluetoothPermissionResultEvent())
        return true
    }

    fun isBluetoothReadyToUse(): Boolean = AndroidBleScanner.isReadyToUse()

    fun isAdvertiseReady(): Boolean = AndroidBleScanner.isAdvertiseReady()

    fun isBlePermissionGranted(): Boolean = AndroidBleScanner.isBlePermissionGranted()

    private fun isBluetoothEnabled(): Boolean {
        return AndroidBleScanner.getBluetoothAdapter().isEnabled
    }

    private fun isLocationEnabled(context: Context): Boolean {
        return context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun isScanConnectGranted(context: Context): Boolean {
        return context.hasPermission(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun isAdvertiseGranted(context: Context): Boolean {
        return context.hasPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
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
}
