package com.ismartcoding.plain.features.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.ismartcoding.lib.channel.receiveEventHandler
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.Job

object BluetoothPermission {
    private lateinit var enableBluetoothActivityLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestBluetoothLocationPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var requestBluetoothScanConnectPermissionLauncher: ActivityResultLauncher<Array<String>>
    private val events = mutableListOf<Job>()

    fun init(activity: ComponentActivity) {
        enableBluetoothActivityLauncher =
            activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == AppCompatActivity.RESULT_OK) {
                    if (BluetoothUtil.isBluetoothReadyToUseWithPermissionRequest()) {
                        sendEvent(BluetoothPermissionResultEvent())
                    }
                } else {
                    sendEvent(BluetoothPermissionResultEvent())
                }
            }

        requestBluetoothLocationPermissionLauncher =
            activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    if (BluetoothUtil.isBluetoothReadyToUseWithPermissionRequest()
                    ) {
                        sendEvent(BluetoothPermissionResultEvent())
                    }
                } else {
                    sendEvent(BluetoothPermissionResultEvent())
                    DialogHelper.showMessage(R.string.location_permission_should_be_enabled)
                }
            }

        requestBluetoothScanConnectPermissionLauncher =
            activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                val isGranted = permissions.entries.all { it.value == true }
                if (isGranted) {
                    if (BluetoothUtil.isBluetoothReadyToUseWithPermissionRequest()
                    ) {
                        sendEvent(BluetoothPermissionResultEvent())
                    }
                } else {
                    sendEvent(BluetoothPermissionResultEvent())
                }
            }

        events.add(
            receiveEventHandler<RequestEnableBluetoothEvent> {
                enableBluetoothActivityLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            },
        )

        events.add(
            receiveEventHandler<RequestScanConnectBluetoothEvent> {
                requestBluetoothScanConnectPermissionLauncher.launch(
                    arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT),
                )
            },
        )

        events.add(
            receiveEventHandler<RequestBluetoothLocationPermissionEvent> {
                requestBluetoothLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            },
        )

        events.add(
            receiveEventHandler<RequestBluetoothLocationGPSPermissionEvent> {
                AlertDialog.Builder(activity)
                    .setTitle(getString(R.string.bluetooth_scan_gps_enable_title))
                    .setMessage(getString(R.string.bluetooth_scan_gps_enable_description))
                    .setPositiveButton(getString(R.string.bluetooth_scan_gps_enable_confirm)) { _, _ ->
                        activity.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                    .setCancelable(false)
                    .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                        if (BluetoothUtil.isBluetoothReadyToUse()) {
                            sendEvent(BluetoothPermissionResultEvent())
                        } else {
                            sendEvent(BluetoothPermissionResultEvent())
                        }
                    }
                    .show()
            },
        )
    }

    fun release() {
        events.forEach {
            it.cancel()
        }
    }
}
