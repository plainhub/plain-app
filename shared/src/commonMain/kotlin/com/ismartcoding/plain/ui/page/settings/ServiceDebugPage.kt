package com.ismartcoding.plain.ui.page.settings
import com.ismartcoding.plain.ui.theme.PlainTheme

import com.ismartcoding.plain.i18n.*

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
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.debug.ServiceDebugInfo
import com.ismartcoding.plain.debug.getServiceDebugInfo
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceDebugPage(navController: NavHostController) {
    var info by remember { mutableStateOf(getServiceDebugInfo()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            info = getServiceDebugInfo()
        }
    }

    PScaffold(
        topBar = {
            PTopAppBar(
                navController = navController,
                title = stringResource(Res.string.service_debug),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(top = paddingValues.calculateTopPadding()),
        ) {
            item { TopSpace() }
            item { HttpServerCard(info) }
            item { VerticalSpace(dp = 16.dp) }
            item { MdnsServerCard(info) }
            item { VerticalSpace(dp = 16.dp) }
            item { DlnaServiceCard(info) }
            item { VerticalSpace(dp = 16.dp) }
            item { BleServiceCard(info) }
            item { VerticalSpace(dp = 16.dp) }
            item { AwareServiceCard(info) }
            item { BottomSpace(paddingValues) }
        }
    }
}

@Composable
private fun HttpServerCard(info: ServiceDebugInfo) {
    Subtitle(stringResource(Res.string.http_server))
    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
        PListItem(
            title = stringResource(Res.string.service_running),
            value = booleanText(info.httpServerRunning),
        )
        PListItem(
            title = stringResource(Res.string.http_server_state),
            value = info.httpServerState,
        )
        PListItem(
            title = stringResource(Res.string.http_port),
            value = info.httpPort.toString(),
        )
        PListItem(
            title = stringResource(Res.string.https_port),
            value = info.httpsPort.toString(),
        )
        PListItem(
            title = stringResource(Res.string.ws_session_count),
            value = info.wsSessionCount.toString(),
        )
        if (info.httpServerError.isNotEmpty()) {
            PListItem(
                title = stringResource(Res.string.http_server_error),
                value = info.httpServerError,
            )
        }
    }
}

@Composable
private fun MdnsServerCard(info: ServiceDebugInfo) {
    Subtitle(stringResource(Res.string.mdns_server))
    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
        PListItem(
            title = stringResource(Res.string.service_running),
            value = booleanText(info.mdnsRunning),
        )
        PListItem(
            title = stringResource(Res.string.mdns_hostname),
            value = info.mdnsHostname.ifEmpty { stringResource(Res.string.not_available) },
        )
    }
}

@Composable
private fun DlnaServiceCard(info: ServiceDebugInfo) {
    Subtitle(stringResource(Res.string.dlna_service))
    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
        PListItem(
            title = stringResource(Res.string.service_running),
            value = booleanText(info.dlnaRunning),
        )
        PListItem(
            title = stringResource(Res.string.dlna_playback_state),
            value = info.dlnaPlaybackState.ifEmpty { stringResource(Res.string.not_available) },
        )
        if (info.dlnaStartError.isNotEmpty()) {
            PListItem(
                title = stringResource(Res.string.dlna_start_error),
                value = info.dlnaStartError,
            )
        }
    }
}

@Composable
private fun BleServiceCard(info: ServiceDebugInfo) {
    Subtitle(stringResource(Res.string.ble_service))
    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
        PListItem(
            title = stringResource(Res.string.service_running),
            value = booleanText(info.bleRunning),
        )
        PListItem(
            title = stringResource(Res.string.ble_client_id),
            value = info.bleClientId.ifEmpty { stringResource(Res.string.not_available) },
        )
        PListItem(
            title = stringResource(Res.string.ble_service_uuid),
            subtitle = info.bleServiceUuid.ifEmpty { stringResource(Res.string.not_available) },
        )
    }
}

@Composable
private fun AwareServiceCard(info: ServiceDebugInfo) {
    Subtitle(stringResource(Res.string.aware_service))
    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
        PListItem(
            title = stringResource(Res.string.service_running),
            value = booleanText(info.awareRunning),
        )
        PListItem(
            title = stringResource(Res.string.aware_attach_status),
            value = info.awareAttachStatus.ifEmpty { stringResource(Res.string.not_available) },
        )
        PListItem(
            title = stringResource(Res.string.aware_discovered_peer_count),
            value = info.awareDiscoveredPeerCount.toString(),
        )
    }
}

@Composable
private fun booleanText(value: Boolean): String =
    stringResource(if (value) Res.string.yes else Res.string.no)
