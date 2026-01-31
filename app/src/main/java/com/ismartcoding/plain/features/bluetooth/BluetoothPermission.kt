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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

object BluetoothPermission {
    private lateinit var enableBluetoothActivityLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestBluetoothLocationPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var requestBluetoothScanConnectPermissionLauncher: ActivityResultLauncher<Array<String>>
    private var collectJob: Job? = null
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

        collectJob?.cancel()
        collectJob = activity.lifecycleScope.launch {
            activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                Channel.sharedFlow.collect { event ->
                    when (event) {
                    is RequestEnableBluetoothEvent -> {
                        enableBluetoothActivityLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                    }
                    is RequestScanConnectBluetoothEvent -> {
                        requestBluetoothScanConnectPermissionLauncher.launch(
                            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT),
                        )
                    }
                    is RequestBluetoothLocationPermissionEvent -> {
                        requestBluetoothLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                    is RequestBluetoothLocationGPSPermissionEvent -> {
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
                    }
                }

                }
            }
        }
    }
}
