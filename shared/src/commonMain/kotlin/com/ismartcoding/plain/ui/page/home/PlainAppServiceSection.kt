package com.ismartcoding.plain.ui.page.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.access_settings
import com.ismartcoding.plain.i18n.plainapp_service_failed
import com.ismartcoding.plain.i18n.plainapp_service_off
import com.ismartcoding.plain.i18n.plainapp_service_off_desc
import com.ismartcoding.plain.i18n.plainapp_service_on
import com.ismartcoding.plain.i18n.relaunch_app
import com.ismartcoding.plain.i18n.start_service
import com.ismartcoding.plain.i18n.stay_online
import com.ismartcoding.plain.i18n.stop_service
import com.ismartcoding.plain.i18n.troubleshoot
import com.ismartcoding.plain.i18n.tune
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.helpers.WebHelper
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.theme.PlainTheme
import com.ismartcoding.plain.ui.theme.blue
import com.ismartcoding.plain.ui.theme.tipsText
import org.jetbrains.compose.resources.stringResource

@Composable
fun PlainAppServiceSection(
    navController: NavHostController,
    mainVM: MainViewModel,
    httpServiceState: HttpServiceState,
    errorMessage: String = "",
    onRestartFix: () -> Unit = {},
    isLoading: Boolean = false,
    onRun: (() -> Unit)? = null,
    onStayOnline: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(PlainTheme.CARD_RADIUS),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp)
                    .padding(start = 24.dp, end = 12.dp, top = 12.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = when (httpServiceState) {
                        HttpServiceState.OFF -> stringResource(Res.string.plainapp_service_off)
                        HttpServiceState.ERROR -> stringResource(Res.string.plainapp_service_failed)
                        else -> stringResource(Res.string.plainapp_service_on)
                    },
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Medium,
                        lineHeight = 24.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (httpServiceState != HttpServiceState.ON) {
                    PIconButton(
                        icon = Res.drawable.tune,
                        contentDescription = stringResource(Res.string.access_settings),
                        tint = MaterialTheme.colorScheme.blue,
                        click = { navController.navigate(Routing.DesktopAccessSettings) })
                }
            }
            Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
                when (httpServiceState) {
                    HttpServiceState.OFF -> {
                        Text(
                            text = stringResource(Res.string.plainapp_service_off_desc),
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)),
                        )
                        VerticalSpace(24.dp)
                        PFilledButton(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(Res.string.start_service),
                            onClick = onRun ?: {},
                            buttonSize = ButtonSize.LARGE,
                            isLoading = isLoading,
                        )
                    }

                    HttpServiceState.ERROR -> {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)),
                        )
                        VerticalSpace(24.dp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            POutlinedButton(
                                modifier = Modifier.weight(1f),
                                text = stringResource(Res.string.troubleshoot),
                                onClick = {
                                    WebHelper.open(
                                        "https://plainapp.app/docs/troubleshooting"
                                    )
                                },
                                buttonSize = ButtonSize.MEDIUM,
                            )
                            PFilledButton(
                                modifier = Modifier.weight(1f),
                                text = stringResource(Res.string.relaunch_app),
                                onClick = onRestartFix,
                                type = ButtonType.TERTIARY,
                                buttonSize = ButtonSize.MEDIUM,
                            )
                        }
                    }

                    HttpServiceState.ON -> {
                        VerticalSpace(8.dp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            POutlinedButton(
                                modifier = Modifier.weight(1f),
                                text = stringResource(Res.string.stay_online),
                                onClick = { onStayOnline?.invoke() },
                                buttonSize = ButtonSize.MEDIUM,
                            )
                            PFilledButton(
                                modifier = Modifier.weight(1f),
                                text = stringResource(Res.string.stop_service),
                                onClick = {
                                    mainVM.enableHttpServer(false)
                                },
                                type = ButtonType.DANGER,
                                buttonSize = ButtonSize.MEDIUM,
                            )
                        }
                    }
                }
            }
        }
    }
}
