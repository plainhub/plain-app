package com.ismartcoding.plain.ui.page.root.contents

import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.events.RequestPermissionsEvent
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.preferences.HttpPortPreference
import com.ismartcoding.plain.preferences.HttpsPortPreference
import com.ismartcoding.plain.ui.base.AlertType
import com.ismartcoding.plain.ui.base.PAlert
import com.ismartcoding.plain.ui.base.PMiniOutlineButton
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.web.HttpServerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
internal fun WebAlerts(
    mainVM: MainViewModel,
    context: android.content.Context,
    scope: CoroutineScope,
    systemAlertWindow: Boolean,
) {
    if (mainVM.httpServerError.isNotEmpty()) {
        PAlert(title = stringResource(id = R.string.error), description = mainVM.httpServerError, AlertType.ERROR) {
            if (HttpServerManager.portsInUse.isNotEmpty()) {
                PMiniOutlineButton(
                    label = stringResource(R.string.change_port),
                    click = {
                        scope.launch(Dispatchers.IO) {
                            if (HttpServerManager.portsInUse.contains(TempData.httpPort)) {
                                HttpPortPreference.putAsync(
                                    context,
                                    HttpServerManager.httpPorts.filter { it != TempData.httpPort }.random()
                                )
                            }
                            if (HttpServerManager.portsInUse.contains(TempData.httpsPort)) {
                                HttpsPortPreference.putAsync(
                                    context,
                                    HttpServerManager.httpsPorts.filter { it != TempData.httpsPort }.random()
                                )
                            }
                            coMain {
                                AlertDialog.Builder(context)
                                    .setTitle(R.string.restart_app_title)
                                    .setMessage(R.string.restart_app_message)
                                    .setPositiveButton(R.string.relaunch_app) { _, _ ->
                                        AppHelper.relaunch(context)
                                    }
                                    .setCancelable(false)
                                    .create()
                                    .show()
                            }
                        }
                    },
                )
            }
            PMiniOutlineButton(
                label = stringResource(R.string.relaunch_app),
                modifier = Modifier.padding(start = 16.dp),
                click = { AppHelper.relaunch(context) },
            )
        }
    } else {
        if (mainVM.isVPNConnected) {
            PAlert(
                title = stringResource(id = R.string.attention),
                description = stringResource(id = R.string.vpn_web_conflict_warning),
                AlertType.WARNING
            )
        }
        if (!systemAlertWindow) {
            PAlert(
                title = stringResource(id = R.string.attention),
                description = stringResource(id = R.string.system_alert_window_warning),
                AlertType.WARNING
            ) {
                PMiniOutlineButton(
                    label = stringResource(R.string.grant_permission),
                    click = { sendEvent(RequestPermissionsEvent(Permission.SYSTEM_ALERT_WINDOW)) },
                )
            }
        }
    }
}
