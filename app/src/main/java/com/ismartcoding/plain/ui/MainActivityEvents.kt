package com.ismartcoding.plain.ui
import com.ismartcoding.plain.preferences.*

import com.ismartcoding.plain.i18n.*

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hasRoute
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.enums.HttpServerState
import com.ismartcoding.plain.events.ChannelInviteReceivedEvent
import com.ismartcoding.plain.events.ConfirmToAcceptLoginEvent
import com.ismartcoding.plain.events.ExportFileEvent
import com.ismartcoding.plain.events.HttpServerStateChangedEvent
import com.ismartcoding.plain.events.IgnoreBatteryOptimizationEvent
import com.ismartcoding.plain.events.PairingCanceledEvent
import com.ismartcoding.plain.events.PairingRequestReceivedEvent
import com.ismartcoding.plain.events.PairingSuccessEvent
import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.events.PickFileEvent
import com.ismartcoding.plain.events.RequestPermissionsEvent
import com.ismartcoding.plain.events.HRequestScreenMirrorAudioEvent
import com.ismartcoding.plain.events.RestartAppEvent
import com.ismartcoding.plain.events.HStartScreenMirrorEvent
import com.ismartcoding.plain.events.HOpenAccessibilitySettingsEvent
import com.ismartcoding.plain.events.HOpenWebSettingsEvent
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.helpers.AppHelper
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
                        DialogHelper.showConfirmDialog(LocaleHelper.getString(Res.string.confirm), LocaleHelper.getString(Res.string.storage_permission_confirm)) {
                            coIO { ApiPermissionsPreference.putAsync(Permission.WRITE_EXTERNAL_STORAGE, true); sendEvent(RequestPermissionsEvent(Permission.WRITE_EXTERNAL_STORAGE)) }
                        }
                    }
                }

                is PermissionsResultEvent -> {
                    // handled by individual feature flows
                }

                is HStartScreenMirrorEvent -> {
                    try {
                        if (event.audio && !Permission.RECORD_AUDIO.can(this@initEvents)) recordAudioForMirror.launch(android.Manifest.permission.RECORD_AUDIO)
                        else screenCapture.launch(mediaProjectionManager.createScreenCaptureIntent())
                    } catch (e: IllegalStateException) {
                        LogCat.e("Error launching screen capture: ${e.message}")
                    }
                }

                is HRequestScreenMirrorAudioEvent -> {
                    try {
                        if (Permission.RECORD_AUDIO.can(this@initEvents)) sendScreenMirrorAudioStatus(true)
                        else recordAudioForMirrorLate.launch(android.Manifest.permission.RECORD_AUDIO)
                    } catch (e: IllegalStateException) {
                        LogCat.e("Error requesting RECORD_AUDIO: ${e.message}")
                    }
                }

                is IgnoreBatteryOptimizationEvent -> {
                    try {
                        ignoreBatteryOptimizationActivityLauncher.launch(Intent().apply {
                            action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS; data = Uri.parse("package:$packageName")
                        })
                    } catch (e: IllegalStateException) {
                        LogCat.e("Error launching battery optimization: ${e.message}")
                    }
                }

                is RestartAppEvent -> {
                    startActivity(Intent(this@initEvents, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK })
                    Runtime.getRuntime().exit(0)
                }

                is HOpenAccessibilitySettingsEvent -> {
                    try {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    } catch (e: Exception) {
                        LogCat.e("Error opening accessibility settings: ${e.message}")
                    }
                }

                is HOpenWebSettingsEvent -> {
                    try {
                        val nav = navControllerState.value
                        val alreadyThere = nav?.currentBackStackEntry?.destination?.hasRoute<Routing.WebSettings>() == true
                        if (AppHelper.foregrounded()) {
                            if (!alreadyThere) nav?.navigate(Routing.WebSettings)
                        } else {
                            val intent = Intent(this@initEvents, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                putExtra("navigate_to_web_settings", true)
                            }
                            startActivity(intent)
                        }
                    } catch (e: Exception) {
                        LogCat.e("Error navigating to WebSettings: ${e.message}")
                    }
                }

                is PickFileEvent -> handlePickFileEvent(event)
                is ExportFileEvent -> handleExportFileEvent(event)
                is ConfirmToAcceptLoginEvent -> {
                    pendingLoginEvent = event
                    openNew()
                }

                is PairingRequestReceivedEvent -> {
                    pendingPairingEvent = event
                    openNew()
                }

                is ChannelInviteReceivedEvent -> {
                    pendingChannelInviteEvent = event
                    openNew()
                }

                is PairingCanceledEvent -> {
                    pendingPairingEvent = null
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
    val webEnabled = WebPreference.getAsync()
    val permEnabled = Permission.NOTIFICATION_LISTENER.isEnabledAsync(this)
    PNotificationListenerService.toggle(this, webEnabled && permEnabled)
}
