package com.ismartcoding.plain.ui.models

import com.ismartcoding.plain.i18n.*

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.helpers.coIO
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
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.preferences.WebPreference
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewModel : ViewModel(), MainViewModelBase {
    override var httpServerError = mutableStateOf("")
    override var httpServerState = mutableStateOf(HttpServerState.OFF)
    override var isVPNConnected = mutableStateOf(false)
    override var ip4s = mutableStateOf(emptyList<String>())
    override var ip4 = mutableStateOf("")
    override var currentRootTab = mutableIntStateOf(0)
    override var pendingLoginEvent = mutableStateOf<ConfirmToAcceptLoginEvent?>(null)
    override var pendingPairingRequest = mutableStateOf<DPairingRequest?>(null)
    // The channel invite currently on top of the back stack (if any). Used by
    // ChannelInviteCanceledEvent handling to pop the right page. Not saved across
    // process death — a fresh invite will re-fire ChannelInviteReceivedEvent.
    override var pendingChannelInvite = mutableStateOf<ChannelInviteReceivedEvent?>(null)

    override fun enableHttpServer(enable: Boolean) {
        viewModelScope.launch {
            WebPreference.putAsync(enable)
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

    override fun syncHttpServerState() {
        viewModelScope.launch {
            val webEnabled = WebPreference.getAsync()
            if (!webEnabled) {
                if (!httpServerState.value.isProcessing()) {
                    httpServerState.value = HttpServerState.OFF
                }
                return@launch
            }

            if (httpServerState.value == HttpServerState.ERROR) {
                return@launch
            }

            if (!httpServerState.value.isProcessing() && httpServerState.value != HttpServerState.ON) {
                httpServerState.value = HttpServerState.STARTING
            }

            val serverUp = checkHttpServerAsync()
            if (serverUp) {
                httpServerError.value = ""
                httpServerState.value = HttpServerState.ON
            } else {
                enableHttpServer(true)
            }
        }
    }
}
