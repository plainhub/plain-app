package com.ismartcoding.plain.ui.home

import com.ismartcoding.plain.platform.network.NetworkEvents

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.features.chat.peer.PeerStatusClient
import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.ui.components.ButtonSize
import com.ismartcoding.plain.platform.system.has
import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.events.RequestPermissionsEvent
import com.ismartcoding.plain.events.WindowFocusChangedEvent
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.grant_permission
import com.ismartcoding.plain.i18n.system_alert_window_warning
import com.ismartcoding.plain.i18n.vpn_web_conflict_warning
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.permissions.Permission
import com.ismartcoding.plain.platform.permissions.isGranted
import com.ismartcoding.plain.ui.base.AlertType
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PAlert
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.media.extensions.collectAsStateValue
import com.ismartcoding.plain.ui.theme.onWarningContainer
import com.ismartcoding.plain.ui.settings.UpdateViewModel
import com.ismartcoding.plain.ui.navigation.MainNavScaffold
import com.ismartcoding.plain.ui.settings.UpdateDialog
import com.ismartcoding.plain.HttpServerManager
import com.ismartcoding.plain.Prefs
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(
    navController: NavHostController,
    updateVM: UpdateViewModel,
    onTabSelected: (Int) -> Unit,
) {
    val network = NetworkEvents.snapshot.collectAsStateValue()
    val isVpnActive = network.vpnActive
    val backgroundServiceEnabled = Prefs.backgroundServiceEnabled.collectAsStateValue()
    var systemAlertWindow by remember { mutableStateOf(Permission.SYSTEM_ALERT_WINDOW.isGranted()) }
    val refreshState = rememberRefreshLayoutState {
        PeerStatusClient.reconnectNow("home_pull_refresh")
        setRefreshState(RefreshContentState.Finished)
    }
    val httpServerError = HttpServerManager.httpServerError.collectAsStateValue()
    val httpServerResourcesError = HttpServerManager.httpServerResourcesError.collectAsStateValue()
    var showStayOnlineOverlay by remember { mutableStateOf(false) }

    val foregroundServiceState = HttpServerManager.foregroundServiceState.collectAsStateValue()
    val foregroundServiceError = HttpServerManager.foregroundServiceError.collectAsStateValue()

    LaunchedEffect(Channel.sharedFlow) {
        Channel.sharedFlow.collect { event ->
            when (event) {
                is PermissionsResultEvent -> {
                    systemAlertWindow = Permission.SYSTEM_ALERT_WINDOW.isGranted()
                }

                is WindowFocusChangedEvent -> {
                    NetworkEvents.changed()
                    systemAlertWindow = Permission.SYSTEM_ALERT_WINDOW.isGranted()
                }
            }
        }
    }

    UpdateDialog(updateVM)

    if (showStayOnlineOverlay) {
        StayOnlineModeOverlay { showStayOnlineOverlay = false }
    }

    MainNavScaffold(
        selectedIndex = 0,
        onTabSelected = onTabSelected,
        topBar = { TopBarHome(navController) },
    ) { paddingValues ->
        PullToRefresh(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            refreshLayoutState = refreshState,
        ) {
            LazyColumn(Modifier.fillMaxSize()) {
                item {
                    TopSpace()
                    if (backgroundServiceEnabled) {
                        if (isVpnActive) {
                            PAlert(
                                description = stringResource(Res.string.vpn_web_conflict_warning),
                                AlertType.WARNING,
                            )
                        }
                        if (!systemAlertWindow) {
                            PAlert(
                                description = stringResource(Res.string.system_alert_window_warning),
                                AlertType.WARNING,
                            ) {
                                PTextButton(
                                    text = stringResource(Res.string.grant_permission),
                                    buttonSize = ButtonSize.SMALL,
                                    contentColor = MaterialTheme.colorScheme.onWarningContainer,
                                    onClick = {
                                        sendEvent(RequestPermissionsEvent(Permission.SYSTEM_ALERT_WINDOW))
                                    },
                                )
                            }
                        }
                    }
                }
                item {
                    if (AppFeatureType.CHECK_UPDATES.has()) {
                        UpdateBanner(updateVM)
                    }
                }
                item {
                    DesktopAccessSection(navController)
                    VerticalSpace(16.dp)
                    PlainAppServiceSection(
                        backgroundServiceEnabled = backgroundServiceEnabled,
                        foregroundServiceState = foregroundServiceState,
                        errorMessage = httpServerError.ifEmpty { httpServerResourcesError }.ifEmpty { foregroundServiceError },
                        onRetry = if (httpServerError.isNotEmpty() || httpServerResourcesError.isNotEmpty()) {
                            { HttpServerManager.restartHttpServer() }
                        } else null,
                        onStayOnline = { showStayOnlineOverlay = true },
                    )
                    VerticalSpace(16.dp)
                    DlnaReceiverSection(navController)
                    VerticalSpace(16.dp)
                }
                item {
                    BottomSpace(paddingValues)
                }
            }
        }
    }
}
