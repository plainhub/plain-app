package com.ismartcoding.plain.ui.page.settings
import com.ismartcoding.plain.ui.theme.PlainTheme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.ismartcoding.plain.lib.mdns.MdnsPacketCapture
import com.ismartcoding.plain.lib.mdns.MdnsPacketDirection
import com.ismartcoding.plain.lib.mdns.MdnsPacketLog
import com.ismartcoding.plain.lib.mdns.MdnsServiceSnapshot
import com.ismartcoding.plain.lib.mdns.MdnsServiceBrowser
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import kotlin.time.Instant
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

/**
 * mDNS protocol debug page. Shows the raw wire data parsed from the
 * `_plainapp._tcp.local` responses (service type, instance, hostname, port,
 * TXT records, IPs) for every device currently known to
 * [MdnsServiceBrowser], plus a live stream of the last 100 received and 100
 * sent mDNS packets. Click a packet row to expand its decoded records.
 *
 * While the page is open it keeps periodic discovery running, enables packet
 * capture, and refreshes every two seconds. Leaving the page disables capture
 * so production overhead stays zero.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MdnsDebugPage(navController: NavHostController) {
    var snapshots by remember { mutableStateOf(emptyList<MdnsServiceSnapshot>()) }
    var packetsIn by remember { mutableStateOf(emptyList<MdnsPacketLog>()) }
    var packetsOut by remember { mutableStateOf(emptyList<MdnsPacketLog>()) }
    var expanded by remember { mutableStateOf<String?>(null) }
    var paused by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    // Discovery may already be running (e.g. the Nearby page); only stop it on
    // exit when this page started it.
    var startedByPage by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        MdnsPacketCapture.setEnabled(true)
        startedByPage = !MdnsDiscoverManager.isDiscovering()
        MdnsDiscoverManager.startPeriodicDiscovery()
        while (true) {
            if (!paused) {
                snapshots = MdnsServiceBrowser.snapshot()
                packetsIn = MdnsPacketCapture.snapshotIn()
                packetsOut = MdnsPacketCapture.snapshotOut()
            }
            delay(2000)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            MdnsPacketCapture.setEnabled(false)
            if (startedByPage) MdnsDiscoverManager.stopPeriodicDiscovery()
        }
    }

    PScaffold(
        topBar = {
            PTopAppBar(
                navController = navController,
                title = stringResource(Res.string.mdns_debug),
                actions = {
                    TextButton(onClick = { paused = !paused }) {
                        Text(text = if (paused) "Resume" else "Pause")
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
        ) {
            item { TopSpace() }

            item {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0; expanded = null },
                        text = { Text("Received (${packetsIn.size})") },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1; expanded = null },
                        text = { Text("Sent (${packetsOut.size})") },
                    )
                }
            }
            item { VerticalSpace(8.dp) }

            val packets = if (selectedTab == 0) packetsIn else packetsOut
            if (packets.isEmpty()) item { PacketEmptyRow() }
            packets.forEach { packet ->
                item {
                    PacketCard(packet, expanded == packetKey(packet)) { expanded = if (expanded == packetKey(packet)) null else packetKey(packet) }
                    VerticalSpace(8.dp)
                }
            }

            item { VerticalSpace(16.dp) }
            item { Subtitle(stringResource(Res.string.mdns_service_type)) }
            if (snapshots.isEmpty()) {
                item {
                    Text(
                        text = stringResource(Res.string.mdns_no_devices),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
                    )
                }
            } else {
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
private fun PacketEmptyRow() {
    Text(
        text = "No packets yet",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun PacketCard(log: MdnsPacketLog, expanded: Boolean, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            val endpoints = if (log.direction == MdnsPacketDirection.IN) {
                "${log.srcIp}:${log.srcPort} → local"
            } else {
                "${log.srcIp} → ${if (log.dstIp.isEmpty()) "multicast" else log.dstIp}:${log.dstPort}"
            }
            val arrow = if (log.direction == MdnsPacketDirection.IN) "◀" else "▶"
            Text(
                text = "$arrow  ${packetTime(log.time)}  $endpoints  (${log.size}B)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = log.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            )
        }
        if (expanded) {
            PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                Text(
                    text = log.detail,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}

@Composable
private fun MdnsDeviceCard(snapshot: MdnsServiceSnapshot) {
    Column {
        Subtitle(snapshot.instanceFqdn)
        PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
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

private fun packetKey(log: MdnsPacketLog): String =
    "${log.direction}-${log.time}-${log.srcIp}:${log.srcPort}-${log.dstIp}:${log.dstPort}-${log.size}"

/** Local time as HH:mm:ss.mmm, readable for packet inspection. */
private fun packetTime(ms: Long): String {
    val t = Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.currentSystemDefault())
    val h = t.hour.toString().padStart(2, '0')
    val m = t.minute.toString().padStart(2, '0')
    val s = t.second.toString().padStart(2, '0')
    val milli = (t.nanosecond / 1_000_000).toString().padStart(3, '0')
    return "$h:$m:$s.$milli"
}