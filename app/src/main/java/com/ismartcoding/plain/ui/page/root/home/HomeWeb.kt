package com.ismartcoding.plain.ui.page.root.home

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.enums.HttpServerState
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.preferences.HttpPortPreference
import com.ismartcoding.plain.preferences.HttpsPortPreference
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.web.HttpServerManager
import kotlinx.coroutines.launch

@Composable
fun HomeWeb(
    context: Context,
    navController: NavHostController,
    mainVM: MainViewModel,
    webEnabled: Boolean,
) {
    val scope = rememberCoroutineScope()
    val state = mainVM.httpServerState

    LaunchedEffect(webEnabled) {
        if (webEnabled) {
            mainVM.syncHttpServerState(context)
        }
    }

    val showSuccess = webEnabled && state == HttpServerState.ON
    val showLoading = state.isProcessing() || (webEnabled && state == HttpServerState.OFF)
    val showError = state == HttpServerState.ERROR
    val errorMessage = buildHomeWebErrorMessage(mainVM)

    val onRestartFix: () -> Unit = {
        scope.launch {
            withIO {
                if (HttpServerManager.portsInUse.contains(TempData.httpPort)) {
                    val nextHttp = HttpServerManager.httpPorts.filter { it != TempData.httpPort }.random()
                    HttpPortPreference.putAsync(context, nextHttp)
                    TempData.httpPort = nextHttp
                }
                if (HttpServerManager.portsInUse.contains(TempData.httpsPort)) {
                    val nextHttps = HttpServerManager.httpsPorts.filter { it != TempData.httpsPort }.random()
                    HttpsPortPreference.putAsync(context, nextHttps)
                    TempData.httpsPort = nextHttps
                }
            }
            AppHelper.relaunch(context)
        }
    }

    when {
        !showSuccess && !showLoading && !showError -> {
            HomeWebEntrySection(
                onRun = {
                    if (!webEnabled && !state.isProcessing()) {
                        mainVM.enableHttpServer(context, true)
                    }
                },
            )
        }

        showLoading -> HomeWebLoadingSection()

        showError -> {
            HomeWebErrorSection(
                context = context,
                errorMessage = errorMessage,
                onRestartFix = onRestartFix,
            )
        }

        showSuccess -> {
            HomeWebSuccessSection(
                context = context,
                navController = navController,
                mainVM = mainVM,
            )
        }
    }
}

private fun buildHomeWebErrorMessage(mainVM: MainViewModel): String {
    return if (HttpServerManager.portsInUse.isNotEmpty()) {
        LocaleHelper.getStringF(
            if (HttpServerManager.portsInUse.size > 1) R.string.http_port_conflict_errors else R.string.http_port_conflict_error,
            "port",
            HttpServerManager.portsInUse.joinToString(", "),
        )
    } else {
        mainVM.httpServerError.ifEmpty { LocaleHelper.getString(R.string.http_server_failed) }
    }
}
