package com.ismartcoding.plain.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.HttpServerManager
import com.ismartcoding.plain.features.settings.ServiceState
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.system.Capability
import com.ismartcoding.plain.platform.system.getDeviceCapabilities
import com.ismartcoding.plain.ui.base.AlertType
import com.ismartcoding.plain.ui.base.PAlert
import com.ismartcoding.plain.ui.base.PExpandableSection
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.components.ButtonType
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.theme.tipsText
import com.ismartcoding.plain.ui.theme.plainColors
import org.jetbrains.compose.resources.stringResource

@Composable
fun PlainAppServiceSection(
    backgroundServiceEnabled: Boolean,
    foregroundServiceState: ServiceState,
    errorMessage: String,
    onRetry: (() -> Unit)?,
    onStayOnline: () -> Unit,
) {
    val supportsBackgroundService = Capability.BACKGROUND_SERVICE in getDeviceCapabilities()
    Column {
        if (supportsBackgroundService) {
            PListItem(
                title = stringResource(Res.string.keep_online_in_background),
                subtitle = stringResource(Res.string.keep_online_in_background_desc),
            ) {
                Switch(
                    colors = SwitchDefaults.plainColors(),
                    checked = backgroundServiceEnabled,
                    enabled = !foregroundServiceState.isProcessing(),
                    onCheckedChange = { HttpServerManager.setBackgroundServiceEnabled(it) },
                )
            }
        }
        if (errorMessage.isNotEmpty()) {
            PAlert(description = errorMessage, type = AlertType.ERROR) {
                if (onRetry != null) {
                    PTextButton(
                        text = stringResource(Res.string.retry),
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        onClick = onRetry,
                    )
                }
            }
        }
        val stayOnlineContent: @Composable () -> Unit = {
            Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
                Text(
                    text = stringResource(if (supportsBackgroundService) Res.string.connection_keeps_dropping_help else Res.string.stay_online_foreground_desc),
                    style = MaterialTheme.typography.tipsText(),
                )
                VerticalSpace(16.dp)
                PFilledButton(
                    type = ButtonType.TONAL,
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(Res.string.stay_online),
                    onClick = onStayOnline,
                )
            }
        }
        if (supportsBackgroundService) {
            var expanded by rememberSaveable { mutableStateOf(false) }
            PExpandableSection(
                toggleModifier = Modifier.padding(horizontal = 16.dp),
                text = stringResource(Res.string.connection_keeps_dropping),
                expanded = expanded,
                stateDescription = stringResource(if (expanded) Res.string.expanded else Res.string.collapsed),
                onClick = { expanded = !expanded },
                fadeContent = true,
                content = stayOnlineContent,
            )
        } else {
            stayOnlineContent()
        }
    }
}
