package com.ismartcoding.plain.ui.models

import com.ismartcoding.plain.i18n.*

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.saveable
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.plain.enums.HttpServerState
import com.ismartcoding.plain.events.ConfirmToAcceptLoginEvent
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.data.DPairingRequest
import com.ismartcoding.plain.events.ChannelInviteReceivedEvent
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.Permissions
import com.ismartcoding.plain.events.StartHttpServerEvent
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.preferences.WebPreference
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.web.HttpServerManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-savedstate#savedstate-compose-state
@OptIn(androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi::class)
class MainViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    var httpServerError by savedStateHandle.saveable { mutableStateOf("") }
    var httpServerState by savedStateHandle.saveable {
        mutableStateOf(HttpServerState.OFF)
    }
    var isVPNConnected by savedStateHandle.saveable { mutableStateOf(false) }
    var ip4s by savedStateHandle.saveable { mutableStateOf(emptyList<String>()) }
    var ip4 by savedStateHandle.saveable { mutableStateOf("") }
    var currentRootTab by savedStateHandle.saveable { mutableIntStateOf(0) }
    var pendingLoginEvent by mutableStateOf<ConfirmToAcceptLoginEvent?>(null)
    var pendingPairingRequest by mutableStateOf<DPairingRequest?>(null)
    // The channel invite currently on top of the back stack (if any). Used by
    // ChannelInviteCanceledEvent handling to pop the right page. Not saved across
    // process death — a fresh invite will re-fire ChannelInviteReceivedEvent.
    var pendingChannelInvite by mutableStateOf<ChannelInviteReceivedEvent?>(null)

    fun enableHttpServer(
        context: Context,
        enable: Boolean,
    ) {
        viewModelScope.launch {
            WebPreference.putAsync(enable)
            if (enable) {
                httpServerError = ""
                if (!httpServerState.isProcessing() && httpServerState != HttpServerState.ON) {
                    httpServerState = HttpServerState.STARTING
                }
                val permission = Permission.POST_NOTIFICATIONS
                if (permission.can(context)) {
                    sendEvent(StartHttpServerEvent())
                } else {
                    DialogHelper.showConfirmDialog(
                        LocaleHelper.getStringAsync(Res.string.confirm),
                        LocaleHelper.getStringAsync(Res.string.foreground_service_notification_prompt)
                    ) {
                        coIO {
                            Permissions.ensureNotificationAsync(context)
                            while (!AppHelper.foregrounded()) {
                                LogCat.d("Waiting for foreground")
                                delay(800)
                            }
                            sendEvent(StartHttpServerEvent())
                        }
                    }
                }
            } else {
                HttpServerManager.stopServiceAsync(context)
            }
        }
    }

    fun syncHttpServerState(context: Context) {
        viewModelScope.launch {
            val webEnabled = WebPreference.getAsync()
            if (!webEnabled) {
                if (!httpServerState.isProcessing()) {
                    httpServerState = HttpServerState.OFF
                }
                return@launch
            }

            if (httpServerState == HttpServerState.ERROR) {
                return@launch
            }

            if (!httpServerState.isProcessing() && httpServerState != HttpServerState.ON) {
                httpServerState = HttpServerState.STARTING
            }

            val serverUp = HttpServerManager.checkServerAsync()
            if (serverUp) {
                httpServerError = ""
                httpServerState = HttpServerState.ON
            } else {
                enableHttpServer(context, true)
            }
        }
    }
}
