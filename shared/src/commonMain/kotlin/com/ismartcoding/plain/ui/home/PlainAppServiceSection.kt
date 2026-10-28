package com.ismartcoding.plain.ui.home

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.features.settings.ServiceState
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.HttpServerManager
import com.ismartcoding.plain.platform.system.Capability
import com.ismartcoding.plain.platform.system.getDeviceCapabilities
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.theme.PlainTheme
import com.ismartcoding.plain.ui.theme.tipsText
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.dark_mode as ui_drawable_dark_mode

@Composable
fun PlainAppServiceSection(
    backgroundServiceEnabled: Boolean,
    foregroundServiceState: ServiceState,
    errorMessage: String,
    onRetry: (() -> Unit)?,
    onStayOnline: () -> Unit,
) {
    val supportsBackgroundService = Capability.BACKGROUND_SERVICE in getDeviceCapabilities()
    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
        if (supportsBackgroundService) {
            PListItem(
                icon = UiRes.drawable.ui_drawable_dark_mode,
                title = {
                    Text(
                        text = stringResource(Res.string.keep_online_in_background),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        autoSize = TextAutoSize.StepBased(
                            minFontSize = MaterialTheme.typography.bodyMedium.fontSize,
                            maxFontSize = MaterialTheme.typography.titleMedium.fontSize,
                        ),
                    )
                },
            ) {
                Switch(
                    checked = backgroundServiceEnabled,
                    enabled = !foregroundServiceState.isProcessing(),
                    onCheckedChange = { HttpServerManager.setBackgroundServiceEnabled(it) },
                )
            }
            Text(
                text = stringResource(Res.string.keep_online_in_background_desc),
                style = MaterialTheme.typography.tipsText(),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        if (errorMessage.isNotEmpty()) {
            PAlert(description = errorMessage, type = AlertType.ERROR) {
                if (onRetry != null) {
                    PTextButton(text = stringResource(Res.string.retry), onClick = onRetry)
                }
            }
        }
        val stayOnlineContent: @Composable () -> Unit = {
            Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = if (supportsBackgroundService) 0.dp else 16.dp, bottom = 8.dp)) {
                Text(
                    text = stringResource(if (supportsBackgroundService) Res.string.connection_keeps_dropping_help else Res.string.stay_online_foreground_desc),
                    style = MaterialTheme.typography.tipsText(),
                )
                VerticalSpace(16.dp)
                POutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(Res.string.stay_online),
                    onClick = onStayOnline,
                )
            }
        }
        if (supportsBackgroundService) {
            var expanded by rememberSaveable { mutableStateOf(false) }
            PExpandableSection(
                toggleModifier = Modifier.padding(horizontal = 8.dp),
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
        VerticalSpace(8.dp)
    }
}
