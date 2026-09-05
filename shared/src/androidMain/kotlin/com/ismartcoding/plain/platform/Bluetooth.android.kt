package com.ismartcoding.plain.platform

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import com.ismartcoding.plain.appContextValue
import com.ismartcoding.plain.ble.client.AndroidBleScanner
import com.ismartcoding.plain.ble.client.BleScanner
import com.ismartcoding.plain.ble.server.AndroidBleGattServer
import com.ismartcoding.plain.ble.server.BleGattServer
import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.features.bluetooth.client.BluetoothPermissionResultEvent
import com.ismartcoding.plain.features.bluetooth.client.BluetoothUtil
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.extensions.hasPermission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private fun computeBleAvailability(): BleAvailability {
    val ctx = appContextValue ?: return BleAvailability.UNSUPPORTED
    if (!ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
        return BleAvailability.UNSUPPORTED
    }
    val adapter = (ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    return if (isSPlus()) {
        when {
            !ctx.hasPermission(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
            ) -> BleAvailability.PERMISSION_DENIED
            adapter?.isEnabled != true -> BleAvailability.BLUETOOTH_OFF
            else -> BleAvailability.READY
        }
    } else {
        when {
            !ctx.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) -> BleAvailability.PERMISSION_DENIED
            adapter?.isEnabled != true -> BleAvailability.BLUETOOTH_OFF
            else -> BleAvailability.READY
        }
    }
}

actual fun bleAvailability(): BleAvailability = computeBleAvailability()

actual val bleAvailabilityFlow: StateFlow<BleAvailability> by lazy {
    MutableStateFlow(computeBleAvailability()).also { flow ->
        appContextValue?.applicationContext?.registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                        flow.value = computeBleAvailability()
                    }
                }
            },
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
        )
        coIO {
            Channel.sharedFlow.collect { event ->
                if (event is BluetoothPermissionResultEvent || event is PermissionsResultEvent) {
                    flow.value = computeBleAvailability()
                }
            }
        }
    }
}

actual suspend fun ensureBlePermissionAsync(): Boolean = BluetoothUtil.ensurePermissionAsync()

actual fun setBluetoothCanContinue(value: Boolean) {
    BluetoothUtil.canContinue = value
}

actual fun bleTransport(): BleTransport = AndroidBleTransport

object AndroidBleTransport : BleTransport {
    override fun createScanner(): BleScanner = AndroidBleScanner

    override fun createServer(): BleGattServer = AndroidBleGattServer()
}
