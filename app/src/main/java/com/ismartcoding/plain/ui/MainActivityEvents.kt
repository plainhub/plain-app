package com.ismartcoding.plain.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.HttpServerState
import com.ismartcoding.plain.events.ChannelInviteReceivedEvent
import com.ismartcoding.plain.events.ConfirmToAcceptLoginEvent
import com.ismartcoding.plain.events.ExportFileEvent
import com.ismartcoding.plain.events.HttpServerStateChangedEvent
import com.ismartcoding.plain.events.IgnoreBatteryOptimizationEvent
import com.ismartcoding.plain.events.PairingCancelledEvent
import com.ismartcoding.plain.events.PairingRequestReceivedEvent
import com.ismartcoding.plain.events.PairingSuccessEvent
import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.events.PickFileEvent
import com.ismartcoding.plain.events.RequestPermissionsEvent
import com.ismartcoding.plain.events.RequestScreenMirrorAudioEvent
import com.ismartcoding.plain.events.RestartAppEvent
import com.ismartcoding.plain.events.StartScreenMirrorEvent
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.mediaProjectionManager
import com.ismartcoding.plain.preferences.ApiPermissionsPreference
import com.ismartcoding.plain.preferences.WebPreference
import com.ismartcoding.plain.services.PNotificationListenerService
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.web.HttpServerManager
import kotlinx.coroutines.launch

@SuppressLint("CheckResult")
internal fun MainActivity.initEvents() {
    lifecycleScope.launch {
        Channel.sharedFlow.collect { event ->
            if (isDestroyed || isFinishing) return@collect

            when (event) {
                is HttpServerStateChangedEvent -> {
                    mainVM.httpServerError = HttpServerManager.httpServerError
                    mainVM.httpServerState = event.state
                    if (event.state == HttpServerState.ON && !Permission.WRITE_EXTERNAL_STORAGE.can(this@initEvents)) {
                        DialogHelper.showConfirmDialog(LocaleHelper.getString(R.string.confirm), LocaleHelper.getString(R.string.storage_permission_confirm)) {
                            coIO { ApiPermissionsPreference.putAsync(this@initEvents, Permission.WRITE_EXTERNAL_STORAGE, true); sendEvent(RequestPermissionsEvent(Permission.WRITE_EXTERNAL_STORAGE)) }
                        }
                    }
                }
                is PermissionsResultEvent -> {
                    // handled by individual feature flows
                }
                is StartScreenMirrorEvent -> {
                    try {
                        if (event.audio && !Permission.RECORD_AUDIO.can(this@initEvents)) recordAudioForMirror.launch(android.Manifest.permission.RECORD_AUDIO)
                        else screenCapture.launch(mediaProjectionManager.createScreenCaptureIntent())
                    } catch (e: IllegalStateException) { LogCat.e("Error launching screen capture: ${e.message}") }
                }
                is RequestScreenMirrorAudioEvent -> {
                    try {
                        if (Permission.RECORD_AUDIO.can(this@initEvents)) sendScreenMirrorAudioStatus(true)
                        else recordAudioForMirrorLate.launch(android.Manifest.permission.RECORD_AUDIO)
                    } catch (e: IllegalStateException) { LogCat.e("Error requesting RECORD_AUDIO: ${e.message}") }
                }
                is IgnoreBatteryOptimizationEvent -> {
                    try {
                        ignoreBatteryOptimizationActivityLauncher.launch(Intent().apply {
                            action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS; data = Uri.parse("package:$packageName")
                        })
                    } catch (e: IllegalStateException) { LogCat.e("Error launching battery optimization: ${e.message}") }
                }
                is RestartAppEvent -> {
                    startActivity(Intent(this@initEvents, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK })
                    Runtime.getRuntime().exit(0)
                }
                is PickFileEvent -> handlePickFileEvent(event)
                is ExportFileEvent -> handleExportFileEvent(event)
                is ConfirmToAcceptLoginEvent -> handleConfirmToAcceptLogin(event)
                is PairingRequestReceivedEvent -> handlePairingRequest(event)
                is ChannelInviteReceivedEvent -> handleChannelInvite(event)
                is PairingCancelledEvent -> {
                    try {
                        if (pairingRequestDialog?.isShowing == true) { pairingRequestDialog?.dismiss(); pairingRequestDialog = null }
                    } catch (e: Exception) { LogCat.e("Error closing pairing dialog: ${e.message}"); pairingRequestDialog = null }
                }
                is PairingSuccessEvent -> {
                    withIO { peerVM.loadPeers() }
                    navControllerState.value?.navigate(Routing.Chat("peer:${event.deviceId}")) { popUpTo<Routing.Nearby> { inclusive = true } }
                }
            }
        }
    }
}

internal suspend fun MainActivity.doWhenReadyAsync() {
    val webEnabled = WebPreference.getAsync(this)
    val permEnabled = Permission.NOTIFICATION_LISTENER.isEnabledAsync(this)
    PNotificationListenerService.toggle(this, webEnabled && permEnabled)
}
