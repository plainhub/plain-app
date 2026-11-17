package com.ismartcoding.plain.ui.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import com.ismartcoding.plain.ui.platform.media.LocalImageRenderer
import com.ismartcoding.plain.ui.platform.media.AndroidImageRenderer
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.NavHostController
import com.ismartcoding.plain.MainActivity
import com.ismartcoding.plain.handleIntent
import com.ismartcoding.plain.ui.navigation.addPlatformPages
import com.ismartcoding.plain.ui.settings.CrashReportDialog
import com.ismartcoding.plain.ui.web.BatteryOptimizationSettings

internal class ActivityUiState {
    val navControllerState = mutableStateOf<NavHostController?>(null)
    var pendingCrashReport by mutableStateOf<String?>(null)
}

internal val MainActivity.navControllerState get() = uiState.navControllerState
internal var MainActivity.pendingCrashReport: String?
    get() = uiState.pendingCrashReport
    set(value) { uiState.pendingCrashReport = value }

internal fun MainActivity.installComposeUi() {
    setContent {
        CompositionLocalProvider(
            LocalImageRenderer provides AndroidImageRenderer,
        ) {
            AppContent(
                navControllerState, onLaunched = { handleIntent(intent) },
                audioQueueVM, pomodoroVM, peerVM, channelVM,
                platformPages = { controller -> addPlatformPages(controller) },
                batterySettings = { BatteryOptimizationSettings() },
            ) {
                pendingCrashReport?.let { report ->
                    CrashReportDialog(crashReport = report, navController = navControllerState.value, onDismiss = { pendingCrashReport = null })
                }
            }
        }
    }
}
