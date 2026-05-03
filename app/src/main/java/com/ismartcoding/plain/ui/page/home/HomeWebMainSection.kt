package com.ismartcoding.plain.ui.page.home

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.theme.blue
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import com.ismartcoding.plain.web.HttpServerManager

@Composable
fun HomeWebMainSection(
    context: Context,
    navController: NavHostController,
    mainVM: MainViewModel,
    webState: WebState,
    errorMessage: String = "",
    onRestartFix: () -> Unit = {},
    isLoading: Boolean = false,
    onRun: (() -> Unit)? = null,
) {
    val onlineCount by HttpServerManager.wsSessionCount.collectAsState()

    Column {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.cardBackgroundNormal,
        ) {
            Column(
                modifier = Modifier.padding(
                    top = 16.dp,
                    start = 24.dp,
                    end = 24.dp,
                    bottom = 24.dp
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = when (webState) {
                            WebState.OFF -> stringResource(R.string.web_portal_off)
                            WebState.ERROR -> stringResource(R.string.home_web_easy_failed_title)
                            else -> stringResource(R.string.web_portal_running)
                        },
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.SemiBold, lineHeight = 36.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (webState != WebState.ON) {
                        PIconButton(
                            icon = R.drawable.tune,
                            contentDescription = stringResource(R.string.web_settings),
                            tint = MaterialTheme.colorScheme.blue,
                            click = { navController.navigate(Routing.WebSettings) })
                    }
                }
                VerticalSpace(12.dp)
                Text(
                    text = when (webState) {
                        WebState.OFF -> stringResource(R.string.web_portal_desc_off)
                        WebState.ERROR -> errorMessage
                        else -> stringResource(R.string.web_portal_desc_running)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                VerticalSpace(24.dp)
                when (webState) {
                    WebState.OFF -> PFilledButton(
                        text = stringResource(R.string.start_service),
                        onClick = onRun ?: {},
                        buttonSize = ButtonSize.LARGE,
                        isLoading = isLoading,
                    )

                    WebState.ERROR -> PFilledButton(
                        text = stringResource(R.string.relaunch_app),
                        onClick = onRestartFix,
                        type = ButtonType.TERTIARY,
                        buttonSize = ButtonSize.LARGE,
                    )

                    else -> PFilledButton(
                        text = stringResource(R.string.stop_service),
                        onClick = {
                            mainVM.enableHttpServer(
                                context,
                                false
                            )
                        },
                        type = ButtonType.DANGER,
                        buttonSize = ButtonSize.LARGE,
                    )
                }
                if (webState == WebState.ON && onlineCount > 0) {
                    VerticalSpace(16.dp)
                    OnlineSessionsIndicator(
                        count = onlineCount,
                        onClick = { navController.navigate(Routing.Connections) })
                }
            }
        }
        if (webState != WebState.OFF) {
            VerticalSpace(12.dp)
            HomeWebAddressSection(context, navController, mainVM, webState == WebState.ERROR)
        }
    }
}

