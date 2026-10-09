package com.ismartcoding.plain.ui.page.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.enums.HttpServerState
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.HttpServerManager
import com.ismartcoding.plain.platform.isAndroidOnly
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.theme.PlainTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun PlainAppServiceSection(
    backgroundEnabled: Boolean,
    backgroundState: HttpServerState,
    errorMessage: String,
    onRetry: () -> Unit,
    onStayOnline: () -> Unit,
) {
    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
        if (isAndroidOnly()) {
            PListItem(
                title = stringResource(Res.string.keep_online_in_background),
                subtitle = stringResource(Res.string.keep_online_in_background_desc),
            ) {
                Switch(
                    checked = backgroundEnabled,
                    enabled = !backgroundState.isProcessing(),
                    onCheckedChange = { HttpServerManager.setBackgroundEnabled(it) },
                )
            }
        }
        if (errorMessage.isNotEmpty()) {
            PAlert(description = errorMessage, type = AlertType.ERROR) {
                PTextButton(text = stringResource(Res.string.retry), onClick = onRetry)
            }
        }
        OnlineConnectionHelp(onStayOnline)
        VerticalSpace(8.dp)
    }
}
