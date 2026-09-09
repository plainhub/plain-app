package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.enums.HttpServerState
import com.ismartcoding.plain.events.ConfirmToAcceptLoginEvent
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.data.DPairingRequest
import com.ismartcoding.plain.events.ChannelInviteReceivedEvent
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.isAndroidOnly
import com.ismartcoding.plain.platform.isGranted
import com.ismartcoding.plain.platform.checkHttpServerAsync
import com.ismartcoding.plain.platform.stopHttpServiceAsync
import com.ismartcoding.plain.events.StartHttpServerEvent
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.httpserver.HttpServerManager
import com.ismartcoding.plain.preferences.ServicePreference
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    // Server lifecycle state and error are owned by HttpServerManager (single
    // source of truth); this ViewModel only delegates the flows so pages can
    // keep referencing mainVM. No copy is kept here — a copy would go stale
    // whenever the writer emits while this ViewModel's collector is gone.
    val httpServerState = HttpServerManager.serverState
    val httpServerError = HttpServerManager.httpServerError
    var isVPNConnected = mutableStateOf(false)
    var currentRootTab = mutableIntStateOf(0)
    var pendingLoginEvent = mutableStateOf<ConfirmToAcceptLoginEvent?>(null)
    var pendingPairingRequest = mutableStateOf<DPairingRequest?>(null)
    // The channel invite currently on top of the back stack (if any). Used by
    // ChannelInviteCanceledEvent handling to pop the right page. Not saved across
    // process death — a fresh invite will re-fire ChannelInviteReceivedEvent.
    var pendingChannelInvite = mutableStateOf<ChannelInviteReceivedEvent?>(null)
    // True while the first-run service permission wizard (ServiceOnboardingWizard)
    // is on screen. Only an explicit user action (the start button) opens it;
    // auto-restore never nags and starts without the prompt instead.
    var showPermissionWizard = mutableStateOf(false)

    fun enableHttpServer(enable: Boolean) {
        viewModelScope.launch {
            val t0 = TimeHelper.nowMillis()
            ServicePreference.putAsync(enable)
            LogCat.d("enableHttpServer($enable): pref write ${TimeHelper.nowMillis() - t0}ms")
            if (enable) {
                startHttpServerWithPermissionFlow(fromUi = true)
            } else {
                stopHttpServiceAsync()
            }
        }
    }

    /**
     * App-open auto-restore: the service preference is already true (the caller
     * checked), so skip the preference write — the first DataStore write of a
     * cold launch costs 600ms+ on the main thread.
     */
    fun restoreHttpServerOnAppOpen() {
        viewModelScope.launch {
            startHttpServerWithPermissionFlow(fromUi = false)
        }
    }

    /**
     * Start the server from a context where the service preference is already
     * true (UI toggle path and the app-open auto-restore in
     * [syncHttpServerState]). Skips the redundant preference write, which
     * costs 600ms+ on a cold DataStore and stalls the main thread.
     */
    private suspend fun startHttpServerWithPermissionFlow(fromUi: Boolean) {
        HttpServerManager.httpServerError.value = ""
        // iOS has no foreground service, so no notification permission is needed.
        if (!isAndroidOnly() || Permission.POST_NOTIFICATIONS.isGranted()) {
            dispatchStartHttpServer()
            return
        }
        if (fromUi) {
            // First run: pace the permission prompts through the wizard instead
            // of firing system dialogs back-to-back.
            showPermissionWizard.value = true
        }
        // Auto-restore with notifications blocked: stay OFF. A foreground
        // service without its notification is invisible and easily killed;
        // the wizard runs on the next explicit tap of the start button.
    }

    // Called by the wizard when the user reaches the final step; permissions
    // are resolved (or explicitly skipped) at that point.
    fun startServiceAfterWizard() {
        dispatchStartHttpServer()
    }

    fun closePermissionWizard() {
        showPermissionWizard.value = false
    }

    // Record the STARTING transition only when the start command is actually
    // dispatched, so a dismissed permission dialog never strands the state.
    private fun dispatchStartHttpServer() {
        LogCat.d("dispatchStartHttpServer")
        HttpServerManager.serverState.value = HttpServerState.STARTING
        sendEvent(StartHttpServerEvent())
    }

    fun syncHttpServerState() {
        viewModelScope.launch {
            val serviceEnabled = ServicePreference.getAsync()
            if (!serviceEnabled) {
                if (!HttpServerManager.serverState.value.isProcessing()) {
                    HttpServerManager.serverState.value = HttpServerState.OFF
                }
                return@launch
            }

            when (HttpServerManager.serverState.value) {
                HttpServerState.ERROR -> return@launch
                // A start/stop orchestration is in flight in this process and
                // will write the terminal state itself; a parallel health check
                // here raced with it and could overwrite a fresh verdict.
                HttpServerState.STARTING, HttpServerState.STOPPING -> return@launch
                HttpServerState.OFF -> {
                    if (showPermissionWizard.value) {
                        // The wizard owns the start decision while it is on
                        // screen; auto-starting here would run the service
                        // behind the wizard's back.
                        return@launch
                    }
                    val serverUp = checkHttpServerAsync()
                    // Apply the verdict only if no writer changed the state meanwhile.
                    if (HttpServerManager.serverState.value == HttpServerState.OFF) {
                        if (serverUp) {
                            HttpServerManager.httpServerError.value = ""
                            HttpServerManager.serverState.value = HttpServerState.ON
                        } else {
                            // Preference is already true here — no rewrite needed.
                            startHttpServerWithPermissionFlow(fromUi = false)
                        }
                    }
                }
                HttpServerState.ON -> {
                    val serverUp = checkHttpServerAsync()
                    if (!serverUp && HttpServerManager.serverState.value == HttpServerState.ON) {
                        HttpServerManager.serverState.value = HttpServerState.ERROR
                    }
                }
            }
        }
    }
}
