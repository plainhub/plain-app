package com.ismartcoding.plain.ui.page.settings

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
import com.ismartcoding.plain.platform.AwareDebugInfo
import com.ismartcoding.plain.platform.getAwareDebugInfo
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiAwareDebugPage(navController: NavHostController) {
    var info by remember { mutableStateOf(getAwareDebugInfo()) }

    // Refresh the snapshot periodically so runtime status (attach/publish/
    // subscribe/discovered-peer-count) stays current while the page is open.
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            info = getAwareDebugInfo()
        }
    }

    PScaffold(
        topBar = {
            PTopAppBar(
                navController = navController,
                title = stringResource(Res.string.wifi_aware_debug),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(top = paddingValues.calculateTopPadding()),
        ) {
            item { TopSpace() }
            item { AwareSupportCard(info) }
            item { VerticalSpace(dp = 16.dp) }
            item { AwareCharacteristicsCard(info) }
            item { VerticalSpace(dp = 16.dp) }
            item { AwarePermissionsCard(info) }
            item { VerticalSpace(dp = 16.dp) }
            item { AwareRuntimeStatusCard(info) }
            item { BottomSpace(paddingValues) }
        }
    }
}

@Composable
private fun AwareSupportCard(info: AwareDebugInfo) {
    SectionTitle(stringResource(Res.string.wifi_aware_support))
    PCard {
        PListItem(
            title = stringResource(Res.string.wifi_aware_supported),
            value = booleanText(info.supported),
        )
        PListItem(
            title = stringResource(Res.string.wifi_aware_available),
            value = booleanText(info.available),
        )
        PListItem(
            title = stringResource(Res.string.wifi_aware_pairing_supported),
            subtitle = stringResource(Res.string.wifi_aware_pairing_supported_desc),
            value = booleanText(info.pairingSupported),
        )
        PListItem(
            title = stringResource(Res.string.wifi_aware_pairing_cipher_suites),
            value = info.supportedPairingCipherSuites.joinToString(", ").ifEmpty { null }
                ?: stringResource(Res.string.not_available),
        )
        PListItem(
            title = stringResource(Res.string.wifi_aware_data_cipher_suites),
            value = info.supportedDataCipherSuites.joinToString(", ").ifEmpty { null }
                ?: stringResource(Res.string.not_available),
        )
    }
}

@Composable
private fun AwareCharacteristicsCard(info: AwareDebugInfo) {
    SectionTitle(stringResource(Res.string.wifi_aware_characteristics))
    PCard {
        PListItem(
            title = stringResource(Res.string.wifi_aware_max_service_name_length),
            value = info.maxServiceNameLength?.toString()
                ?: stringResource(Res.string.not_available),
        )
        PListItem(
            title = stringResource(Res.string.wifi_aware_max_service_specific_info_length),
            value = info.maxServiceSpecificInfoLength?.toString()
                ?: stringResource(Res.string.not_available),
        )
        PListItem(
            title = stringResource(Res.string.wifi_aware_max_match_filter_length),
            value = info.maxMatchFilterLength?.toString()
                ?: stringResource(Res.string.not_available),
        )
        PListItem(
            title = stringResource(Res.string.wifi_aware_max_extended_service_specific_info_length),
            value = info.maxExtendedServiceSpecificInfoLength?.toString()
                ?: stringResource(Res.string.not_available),
        )
    }
}

@Composable
private fun AwarePermissionsCard(info: AwareDebugInfo) {
    SectionTitle(stringResource(Res.string.wifi_aware_permissions))
    PCard {
        PListItem(
            title = stringResource(Res.string.wifi_aware_nearby_devices_permission),
            value = booleanText(info.nearbyDevicesPermissionGranted),
        )
        PListItem(
            title = stringResource(Res.string.wifi_aware_location_permission),
            value = booleanText(info.locationPermissionGranted),
        )
        PListItem(
            title = stringResource(Res.string.wifi_aware_wifi_enabled),
            value = booleanText(info.wifiEnabled),
        )
    }
}

@Composable
private fun AwareRuntimeStatusCard(info: AwareDebugInfo) {
    SectionTitle(stringResource(Res.string.wifi_aware_runtime_status))
    PCard {
        PListItem(
            title = stringResource(Res.string.wifi_aware_attach_status),
            value = info.attachStatus.ifEmpty { stringResource(Res.string.not_available) },
        )
        PListItem(
            title = stringResource(Res.string.wifi_aware_publish_session_status),
            value = info.publishSessionStatus.ifEmpty { stringResource(Res.string.not_available) },
        )
        PListItem(
            title = stringResource(Res.string.wifi_aware_subscribe_session_status),
            value = info.subscribeSessionStatus.ifEmpty { stringResource(Res.string.not_available) },
        )
        PListItem(
            title = stringResource(Res.string.wifi_aware_discovered_peer_count),
            value = info.discoveredPeerCount.toString(),
        )
    }
}

@Composable
private fun booleanText(value: Boolean): String =
    stringResource(if (value) Res.string.yes else Res.string.no)
