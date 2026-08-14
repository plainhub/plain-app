package com.ismartcoding.plain.ui.page.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.discover.MdnsDiscoverManager
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.mdns_debug
import com.ismartcoding.plain.i18n.mdns_hostname
import com.ismartcoding.plain.i18n.mdns_instance
import com.ismartcoding.plain.i18n.mdns_ips
import com.ismartcoding.plain.i18n.mdns_no_devices
import com.ismartcoding.plain.i18n.mdns_port
import com.ismartcoding.plain.i18n.mdns_service_type
import com.ismartcoding.plain.i18n.mdns_txt
import com.ismartcoding.plain.i18n.not_available
import com.ismartcoding.plain.mdns.MdnsServiceSnapshot
import com.ismartcoding.plain.mdns.MdnsServiceBrowser
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/**
 * mDNS protocol debug page. Shows the raw wire data parsed from the
 * `_plainapp._tcp.local` responses (service type, instance, hostname, port,
 * TXT records, IPs) for every device currently known to
 * [MdnsServiceBrowser], so the RFC 6762 implementation can be inspected
 * for correctness (see the design doc §4).
 *
 * While the page is open it keeps periodic discovery running and refreshes
 * the snapshot every two seconds.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MdnsDebugPage(navController: NavHostController) {
    var snapshots by remember { mutableStateOf(emptyList<MdnsServiceSnapshot>()) }

    // Discovery may already be running (e.g. the Nearby page); only stop it on
    // exit when this page started it.
    var startedByPage by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        startedByPage = !MdnsDiscoverManager.isDiscovering()
        MdnsDiscoverManager.startPeriodicDiscovery()
        while (true) {
            snapshots = MdnsServiceBrowser.snapshot()
            delay(2000)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (startedByPage) MdnsDiscoverManager.stopPeriodicDiscovery()
        }
    }

    PScaffold(
        topBar = {
            PTopAppBar(
                navController = navController,
                title = stringResource(Res.string.mdns_debug),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
        ) {
            if (snapshots.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(Res.string.mdns_no_devices),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp),
                        )
                    }
                }
            } else {
                item { TopSpace() }
                snapshots.forEach { snapshot ->
                    item {
                        MdnsDeviceCard(snapshot)
                        VerticalSpace(16.dp)
                    }
                }
            }
            item { BottomSpace(paddingValues) }
        }
    }
}

@Composable
private fun MdnsDeviceCard(snapshot: MdnsServiceSnapshot) {
    Column {
        Subtitle(snapshot.instanceFqdn)
        PCard {
            PListItem(
                title = stringResource(Res.string.mdns_service_type),
                value = snapshot.serviceType.ifEmpty { null }
                    ?: stringResource(Res.string.not_available),
            )
            PListItem(
                title = stringResource(Res.string.mdns_instance),
                value = snapshot.instanceName.ifEmpty { null }
                    ?: stringResource(Res.string.not_available),
            )
            PListItem(
                title = stringResource(Res.string.mdns_hostname),
                value = snapshot.hostname.ifEmpty { null }
                    ?: stringResource(Res.string.not_available),
            )
            PListItem(
                title = stringResource(Res.string.mdns_port),
                value = if (snapshot.port > 0) snapshot.port.toString()
                else stringResource(Res.string.not_available),
            )
            PListItem(
                title = stringResource(Res.string.mdns_ips),
                value = snapshot.ips.joinToString(", ").ifEmpty { null }
                    ?: stringResource(Res.string.not_available),
            )
            PListItem(
                title = stringResource(Res.string.mdns_txt),
                value = snapshot.txtRecords.joinToString("\n").ifEmpty { null }
                    ?: stringResource(Res.string.not_available),
            )
        }
    }
}
