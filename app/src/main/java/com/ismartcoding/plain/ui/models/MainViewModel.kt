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
import com.ismartcoding.plain.db.DBox
import com.ismartcoding.plain.enums.HttpServerState
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.Permissions
import com.ismartcoding.plain.features.StartHttpServerEvent
import com.ismartcoding.plain.features.box.BoxHelper
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.preference.WebPreference
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.web.HttpServerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-savedstate#savedstate-compose-state
@OptIn(androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi::class)
class MainViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val _boxes = MutableStateFlow(listOf<DBox>())
    val boxes: StateFlow<List<DBox>> get() = _boxes.asStateFlow()
    var httpServerError by savedStateHandle.saveable { mutableStateOf("") }
    var httpServerState by savedStateHandle.saveable {
        mutableStateOf(HttpServerState.OFF)
    }

    fun fetch() {
        viewModelScope.launch(Dispatchers.IO) {
            _boxes.value = BoxHelper.getItemsAsync()
        }
    }

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
