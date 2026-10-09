package com.ismartcoding.plain.ui.page.home

import com.ismartcoding.plain.preferences.*

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.peer.PeerStatusManager
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.enums.HttpServerState
import com.ismartcoding.plain.enums.has
import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.events.RequestPermissionsEvent
import com.ismartcoding.plain.events.WindowFocusChangedEvent
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.grant_permission
import com.ismartcoding.plain.i18n.system_alert_window_warning
import com.ismartcoding.plain.i18n.vpn_web_conflict_warning
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.getDeviceIP4s
import com.ismartcoding.plain.platform.isAndroidOnly
import com.ismartcoding.plain.platform.isGranted
import com.ismartcoding.plain.platform.isVPNConnected
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
import com.ismartcoding.plain.ui.extensions.collectAsStateValue
import com.ismartcoding.plain.ui.models.ChannelViewModel
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.models.PeerViewModel
import com.ismartcoding.plain.ui.models.UpdateViewModel
import com.ismartcoding.plain.ui.page.MainNavScaffold
import com.ismartcoding.plain.ui.page.settings.UpdateDialog
import com.ismartcoding.plain.platform.HttpServerManager
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(
    navController: NavHostController,
    mainVM: MainViewModel,
    updateVM: UpdateViewModel,
    peerVM: PeerViewModel,
    channelVM: ChannelViewModel,
    onTabSelected: (Int) -> Unit,
) {
    val serviceEnabled = UserPrefs.service.collectAsStateValue()
    var systemAlertWindow by remember { mutableStateOf(Permission.SYSTEM_ALERT_WINDOW.isGranted()) }
    val refreshState = rememberRefreshLayoutState {
        PeerStatusManager.reconnectNow("home_pull_refresh")
        peerVM.load()
        channelVM.load()
        setRefreshState(RefreshContentState.Finished)
    }
    val serverError = HttpServerManager.httpServerError.collectAsStateValue()
    var showStayOnlineOverlay by remember { mutableStateOf(false) }

    val backgroundState = HttpServerManager.backgroundState.collectAsStateValue()
    val backgroundError = HttpServerManager.backgroundError.collectAsStateValue()
    LaunchedEffect(Unit) { HttpServerManager.ensureStarted() }

    LaunchedEffect(Channel.sharedFlow) {
        Channel.sharedFlow.collect { event ->
            if (updateVM.consumeUpdateDownloadEvent(event)) {
                return@collect
            }

            when (event) {
                is PermissionsResultEvent -> {
                    systemAlertWindow = Permission.SYSTEM_ALERT_WINDOW.isGranted()
                    if (event.map.containsKey(Permission.POST_NOTIFICATIONS.toSysPermission())) {
                        if (Permission.POST_NOTIFICATIONS.isGranted()) HttpServerManager.ensureStarted()
                        else HttpServerManager.setServiceEnabled(false)
                    }
                }

                is WindowFocusChangedEvent -> {
                    mainVM.isVPNConnected.value = isVPNConnected()
                    val ips = getDeviceIP4s().filter { it.isNotEmpty() }
                    TempData.ip4s.value = ips
                    systemAlertWindow = Permission.SYSTEM_ALERT_WINDOW.isGranted()
                    if (event.hasFocus) {
                        HttpServerManager.ensureStarted()
                    }
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
                    if (serviceEnabled) {
                        if (mainVM.isVPNConnected.value) {
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
                    PlainAppServiceSection(
                        backgroundEnabled = backgroundState == HttpServerState.ON || backgroundState == HttpServerState.STARTING,
                        backgroundState = backgroundState,
                        errorMessage = serverError.ifEmpty { backgroundError },
                        onRetry = {
                            if (backgroundState == HttpServerState.ERROR) HttpServerManager.setServiceEnabled(true)
                            else HttpServerManager.ensureStarted()
                        },
                        onStayOnline = { showStayOnlineOverlay = true },
                    )
                    VerticalSpace(16.dp)
                    DesktopAccessSection(navController)
                    VerticalSpace(16.dp)
                    DlnaReceiverSection(navController)
                    VerticalSpace(dp = 16.dp)
                }
                item {
                    BottomSpace(paddingValues)
                }
            }
        }
    }
}
