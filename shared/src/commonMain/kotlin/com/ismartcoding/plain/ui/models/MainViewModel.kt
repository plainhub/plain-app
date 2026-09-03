package com.ismartcoding.plain.ui.models

import com.ismartcoding.plain.i18n.*

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.enums.HttpServerState
import com.ismartcoding.plain.events.ConfirmToAcceptLoginEvent
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.data.DPairingRequest
import com.ismartcoding.plain.events.ChannelInviteReceivedEvent
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.isGranted
import com.ismartcoding.plain.platform.ensureNotificationPermissionAsync
import com.ismartcoding.plain.platform.isAppForegrounded
import com.ismartcoding.plain.platform.checkHttpServerAsync
import com.ismartcoding.plain.platform.stopHttpServiceAsync
import com.ismartcoding.plain.events.StartHttpServerEvent
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.preferences.ServicePreference
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    var httpServerError = mutableStateOf("")
    var httpServerState = mutableStateOf(HttpServerState.OFF)
    var isVPNConnected = mutableStateOf(false)
    var currentRootTab = mutableIntStateOf(0)
    var pendingLoginEvent = mutableStateOf<ConfirmToAcceptLoginEvent?>(null)
    var pendingPairingRequest = mutableStateOf<DPairingRequest?>(null)
    // The channel invite currently on top of the back stack (if any). Used by
    // ChannelInviteCanceledEvent handling to pop the right page. Not saved across
    // process death — a fresh invite will re-fire ChannelInviteReceivedEvent.
    var pendingChannelInvite = mutableStateOf<ChannelInviteReceivedEvent?>(null)

    fun enableHttpServer(enable: Boolean) {
        viewModelScope.launch {
            ServicePreference.putAsync(enable)
            if (enable) {
                httpServerError.value = ""
                if (!httpServerState.value.isProcessing() && httpServerState.value != HttpServerState.ON) {
                    httpServerState.value = HttpServerState.STARTING
                }
                val permission = Permission.POST_NOTIFICATIONS
                if (permission.isGranted()) {
                    sendEvent(StartHttpServerEvent())
                } else {
                    DialogHelper.showConfirmDialog(
                        LocaleHelper.getStringAsync(Res.string.confirm),
                        LocaleHelper.getStringAsync(Res.string.foreground_service_notification_prompt)
                    ) {
                        coIO {
                            ensureNotificationPermissionAsync()
                            while (!isAppForegrounded()) {
                                LogCat.d("Waiting for foreground")
                                delay(800)
                            }
                            sendEvent(StartHttpServerEvent())
                        }
                    }
                }
            } else {
                stopHttpServiceAsync()
            }
        }
    }

    fun syncHttpServerState() {
        viewModelScope.launch {
            val serviceEnabled = ServicePreference.getAsync()
            if (!serviceEnabled) {
                if (!httpServerState.value.isProcessing()) {
                    httpServerState.value = HttpServerState.OFF
                }
                return@launch
            }

            when (httpServerState.value) {
                HttpServerState.ERROR -> return@launch
                // A start/stop orchestration is in flight and its state event is
                // authoritative; running a parallel health check here raced with
                // the service-side check and could leave the UI ON on a dead server.
                HttpServerState.STARTING, HttpServerState.STOPPING -> return@launch
                HttpServerState.OFF -> {
                    httpServerState.value = HttpServerState.STARTING
                    val serverUp = checkHttpServerAsync()
                    // Apply the verdict only if no server event changed the state meanwhile.
                    if (httpServerState.value == HttpServerState.STARTING) {
                        if (serverUp) {
                            httpServerError.value = ""
                            httpServerState.value = HttpServerState.ON
                        } else {
                            enableHttpServer(true)
                        }
                    }
                }
                HttpServerState.ON -> {
                    val serverUp = checkHttpServerAsync()
                    if (!serverUp && httpServerState.value == HttpServerState.ON) {
                        enableHttpServer(true)
                    }
                }
            }
        }
    }
}
