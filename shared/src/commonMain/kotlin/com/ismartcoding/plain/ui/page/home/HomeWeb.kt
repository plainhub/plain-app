package com.ismartcoding.plain.ui.page.home

import com.ismartcoding.plain.i18n.*

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.enums.HttpServerState
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.platform.httpServerPortsInUse
import com.ismartcoding.plain.platform.relaunchApp
import com.ismartcoding.plain.preferences.HttpPortPreference
import com.ismartcoding.plain.preferences.HttpsPortPreference
import com.ismartcoding.plain.ui.models.MainViewModelBase
import com.ismartcoding.plain.web.httpPorts
import com.ismartcoding.plain.web.httpsPorts
import kotlinx.coroutines.launch

enum class WebState { OFF, ERROR, ON }

@Composable
fun HomeWeb(
    navController: NavHostController,
    mainVM: MainViewModelBase,
    webEnabled: Boolean,
) {
    val scope = rememberCoroutineScope()
    val state = mainVM.httpServerState.value

    LaunchedEffect(webEnabled) {
        if (webEnabled) {
            mainVM.syncHttpServerState()
        }
    }

    val showSuccess = webEnabled && state == HttpServerState.ON
    val showLoading = state.isProcessing() || (webEnabled && state == HttpServerState.OFF)
    val showError = state == HttpServerState.ERROR
    val errorMessage = buildHomeWebErrorMessage(mainVM)
    val portsInUse = httpServerPortsInUse()

    val onRestartFix: () -> Unit = {
        scope.launch {
            withIO {
                if (portsInUse.contains(TempData.httpPort.value)) {
                    val nextHttp =
                        httpPorts.filter { it != TempData.httpPort.value }.random()
                    HttpPortPreference.putAsync(nextHttp)
                }
                if (portsInUse.contains(TempData.httpsPort.value)) {
                    val nextHttps =
                        httpsPorts.filter { it != TempData.httpsPort.value }.random()
                    HttpsPortPreference.putAsync(nextHttps)
                }
            }
            relaunchApp()
        }
    }

    val webState = when {
        showSuccess -> WebState.ON
        showError -> WebState.ERROR
        else -> WebState.OFF
    }

    AnimatedContent(
        targetState = webState,
        transitionSpec = {
            fadeIn(tween(300)) togetherWith fadeOut(tween(200)) using
                    SizeTransform(clip = false, sizeAnimationSpec = { _, _ -> tween(300) })
        },
        label = "web_state",
    ) { target ->
        HomeWebMainSection(
            navController = navController,
            mainVM = mainVM,
            webState = target,
            isLoading = showLoading,
            onRun = {
                if (!webEnabled && !state.isProcessing()) {
                    mainVM.enableHttpServer(true)
                }
            },
            errorMessage = errorMessage,
            onRestartFix = onRestartFix,
        )
    }
}

private fun buildHomeWebErrorMessage(mainVM: MainViewModelBase): String {
    val portsInUse = httpServerPortsInUse()
    return if (portsInUse.isNotEmpty()) {
        LocaleHelper.getStringF(
            if (portsInUse.size > 1) Res.string.http_port_conflict_errors else Res.string.http_port_conflict_error,
            "port",
            portsInUse.joinToString(", "),
        )
    } else {
        mainVM.httpServerError.value.ifEmpty { LocaleHelper.getString(Res.string.http_server_failed) }
    }
}
