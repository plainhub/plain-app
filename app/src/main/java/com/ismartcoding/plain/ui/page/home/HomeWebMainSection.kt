package com.ismartcoding.plain.ui.page.home
import com.ismartcoding.plain.preferences.*

import com.ismartcoding.plain.i18n.*
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.preferences.HomeSectionCollapsedPreference
import com.ismartcoding.plain.preferences.dataFlow
import com.ismartcoding.plain.preferences.dataStore
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.extensions.collectAsStateValue
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.theme.PlainTheme
import com.ismartcoding.plain.ui.theme.blue
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import com.ismartcoding.plain.web.HttpServerManager
import kotlinx.coroutines.flow.map

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
    val collapsed = remember {
        context.dataStore.dataFlow.map { HomeSectionCollapsedPreference.get(it, AppFeatureType.WEB_PORTAL) }
    }.collectAsStateValue(initial = false)

    Column {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(PlainTheme.CARD_RADIUS),
            color = MaterialTheme.colorScheme.cardBackgroundNormal,
        ) {
            Column {
                HomeSectionClickableHeader(
                    title = when (webState) {
                        WebState.OFF -> stringResource(Res.string.web_portal_off)
                        WebState.ERROR -> stringResource(Res.string.home_web_easy_failed_title)
                        else -> stringResource(Res.string.web_portal_running)
                    },
                    onClick = { navController.navigate(Routing.WebSettings) },
                    trailingContent = {
                        if (webState != WebState.ON) {
                            PIconButton(
                                icon = Res.drawable.tune,
                                contentDescription = stringResource(Res.string.web_settings),
                                tint = MaterialTheme.colorScheme.blue,
                                click = { navController.navigate(Routing.WebSettings) })
                        } else {
                            HomeSectionCollapseButton(
                                collapsed = collapsed,
                                featureType = AppFeatureType.WEB_PORTAL,
                            )
                        }
                    },
                )
                if (webState != WebState.ON || !collapsed) {
                    Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
                        Text(
                            text = when (webState) {
                                WebState.OFF -> stringResource(Res.string.web_portal_desc_off)
                                WebState.ERROR -> errorMessage
                                else -> stringResource(Res.string.web_portal_desc_running)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        VerticalSpace(24.dp)
                        when (webState) {
                            WebState.OFF -> PFilledButton(
                                text = stringResource(Res.string.start_service),
                                onClick = onRun ?: {},
                                buttonSize = ButtonSize.LARGE,
                                isLoading = isLoading,
                            )

                            WebState.ERROR -> PFilledButton(
                                text = stringResource(Res.string.relaunch_app),
                                onClick = onRestartFix,
                                type = ButtonType.TERTIARY,
                                buttonSize = ButtonSize.LARGE,
                            )

                            else -> PFilledButton(
                                text = stringResource(Res.string.stop_service),
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
            }
        }
        if (webState != WebState.OFF && (webState != WebState.ON || !collapsed)) {
            VerticalSpace(12.dp)
            HomeWebAddressSection(context, navController, mainVM, webState == WebState.ERROR)
        }
    }
}
