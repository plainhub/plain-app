package com.ismartcoding.plain

import android.Manifest

import com.ismartcoding.plain.i18n.*

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hasRoute
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.chat.peer.PeerStatusManager
import com.ismartcoding.plain.enums.HttpServerState
import com.ismartcoding.plain.events.ChannelInviteCanceledEvent
import com.ismartcoding.plain.events.ChannelInviteReceivedEvent
import com.ismartcoding.plain.events.ConfirmToAcceptLoginEvent
import com.ismartcoding.plain.features.dlna.DlnaCastRequestEvent
import com.ismartcoding.plain.httpserver.HttpServerManager
import com.ismartcoding.plain.events.ExportFileEvent
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
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.isGranted
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.preferences.ApiPermissionsPreference
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.nav.Routing
import kotlinx.coroutines.launch

@SuppressLint("CheckResult")
internal fun MainActivity.initEvents() {
    lifecycleScope.launch {
        Channel.sharedFlow.collect { event ->
            if (isDestroyed || isFinishing) return@collect

            when (event) {
                is PermissionsResultEvent -> {
                    // handled by individual feature flows
                }

                is HStartScreenMirrorEvent -> {
                    try {
                        if (event.audio && !Permission.RECORD_AUDIO.isGranted()) {
                            recordAudioForMirror.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            screenCapture.launch(mediaProjectionManager.createScreenCaptureIntent())
                        }
                    } catch (e: IllegalStateException) {
                        LogCat.e("Error launching screen capture: ${e.message}")
                    }
                }

                is HRequestScreenMirrorAudioEvent -> {
                    try {
                        if (Permission.RECORD_AUDIO.isGranted()) sendScreenMirrorAudioStatus(true)
                        else recordAudioForMirrorLate.launch(Manifest.permission.RECORD_AUDIO)
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
                        val alreadyThere = nav?.currentBackStackEntry?.destination?.hasRoute<Routing.DesktopAccessSettings>() == true
                        if (AppHelper.foregrounded()) {
                            if (!alreadyThere) nav?.navigate(Routing.DesktopAccessSettings)
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
                    openNew()
                }

                is DlnaCastRequestEvent -> {
                    // A DLNA cast is being handled — bring the app to the foreground
                    // when it is in the background so the request dialog / player
                    // appears and playback starts immediately (same as login requests).
                    if (!AppHelper.foregrounded()) {
                        openNew()
                    }
                }

                is PairingRequestReceivedEvent -> {
                    mainVM.pendingPairingRequest.value = event.request
                    val nav = navControllerState.value
                    if (nav?.currentBackStackEntry?.destination?.hasRoute<Routing.PairingRequest>() != true) {
                        nav?.navigate(Routing.PairingRequest)
                    }
                    if (!AppHelper.foregrounded()) {
                        openNew()
                    }
                }

                is ChannelInviteReceivedEvent -> {
                    val nav = navControllerState.value
                    if (nav?.currentBackStackEntry?.destination?.hasRoute<Routing.ChannelInviteRequest>() != true) {
                        nav?.navigate(
                            Routing.ChannelInviteRequest(
                                channelId = event.channelId,
                                channelName = event.channelName,
                                ownerPeerId = event.ownerPeerId,
                                ownerPeerName = event.ownerPeerName,
                            )
                        )
                    }
                    openNew()
                }

                is PairingCanceledEvent -> {
                    val nav = navControllerState.value
                    val current = nav?.currentBackStackEntry
                    if (current != null && current.destination.hasRoute<Routing.PairingRequest>() &&
                        mainVM.pendingPairingRequest.value?.fromId == event.fromId
                    ) {
                        nav.popBackStack<Routing.PairingRequest>(inclusive = true)
                    }
                }

                is ChannelInviteCanceledEvent -> {
                    val nav = navControllerState.value
                    val current = nav?.currentBackStackEntry
                    if (current != null && current.destination.hasRoute<Routing.ChannelInviteRequest>() &&
                        mainVM.pendingChannelInvite.value?.channelId == event.channelId
                    ) {
                        nav.popBackStack<Routing.ChannelInviteRequest>(inclusive = true)
                    }
                }

                is PairingSuccessEvent -> {
                    PeerStatusManager.reconnectNow("post_pairing")
                }
            }
        }
    }

    // Android-specific storage permission prompt keyed off the server-state
    // source of truth. Only transitions into ON observed while this collector
    // is alive trigger it (the flow's initial replay is skipped), matching the
    // old HttpServerStateChangedEvent semantics.
    lifecycleScope.launch {
        var previous: HttpServerState? = null
        HttpServerManager.serverState.collect { state ->
            val justTurnedOn = previous != null && previous != HttpServerState.ON && state == HttpServerState.ON
            previous = state
            if (!justTurnedOn || isDestroyed || isFinishing) return@collect
            if (!Permission.WRITE_EXTERNAL_STORAGE.isGranted()) {
                DialogHelper.showConfirmDialog(LocaleHelper.getStringAsync(Res.string.confirm), LocaleHelper.getStringAsync(Res.string.storage_permission_confirm)) {
                    coIO { ApiPermissionsPreference.putAsync(Permission.WRITE_EXTERNAL_STORAGE, true); sendEvent(RequestPermissionsEvent(Permission.WRITE_EXTERNAL_STORAGE)) }
                }
            }
        }
    }
}

