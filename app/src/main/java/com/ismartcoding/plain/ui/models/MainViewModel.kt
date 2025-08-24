package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.saveable
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.HttpServerState
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

    fun enableHttpServer(
        context: Context,
        enable: Boolean,
    ) {
        viewModelScope.launch {
            withIO { WebPreference.putAsync(context, enable) }
            if (enable) {
                val permission = Permission.POST_NOTIFICATIONS
                if (permission.can(context)) {
                    sendEvent(StartHttpServerEvent())
                } else {
                    DialogHelper.showConfirmDialog(
                        LocaleHelper.getString(R.string.confirm),
                        LocaleHelper.getString(R.string.foreground_service_notification_prompt)
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
                withIO {
                    HttpServerManager.stopServiceAsync(context)
                }
            }
        }
    }
}
