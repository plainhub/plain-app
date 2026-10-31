package com.ismartcoding.plain.models

import com.ismartcoding.plain.features.nearby.DiscoveryMethod
import com.ismartcoding.plain.features.nearby.MdnsPacketDirection
import com.ismartcoding.plain.features.nearby.MdnsSnapshot
import com.ismartcoding.plain.features.nearby.PairingPhase
import com.ismartcoding.plain.features.nearby.PairingState
import com.ismartcoding.plain.features.nearby.PeerStatus
import com.ismartcoding.plain.features.notifications.NotificationFilterData
import com.ismartcoding.plain.features.notifications.NotificationFilterMode
import com.ismartcoding.plain.features.settings.AppUpdateDownload
import com.ismartcoding.plain.features.settings.AppUpdateDownloadStatus
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.platform.system.DeviceType
import com.ismartcoding.plain.platform.system.DataType
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class ClientContractModelsTest {
    @Test
    fun tag_types_preserve_the_numeric_graphql_field() {
        val tag = JsonHelper.jsonDecode<Tag>("""{"id":"tag","name":"Label","count":1,"type":6}""")
        assertEquals(DataType.NOTE, tag.type)
        assertEquals(JsonPrimitive(6), JsonHelper.jsonEncodeToElement(tag).jsonObject.getValue("type"))
    }

    @Test
    fun nearby_devices_decode_every_supported_discovery_method() {
        val device = JsonHelper.jsonDecode<NearbyDevice>(
            """{"id":"device","name":"Device","port":8080,"deviceType":"PHONE","version":"1","platform":"android","lastSeen":"2026-01-01T00:00:00Z","status":"UNPAIRED","discoveryMethods":["LAN","DNS_SD","BLE","QR"]}""",
        )
        assertEquals(DeviceType.PHONE, device.deviceType)
        assertEquals(PeerStatus.UNPAIRED, device.status)
        assertEquals(
            listOf(DiscoveryMethod.LAN, DiscoveryMethod.DNS_SD, DiscoveryMethod.BLE, DiscoveryMethod.QR),
            device.discoveryMethods,
        )
    }

    @Test
    fun unknown_nearby_metadata_preserves_the_existing_display_behavior() {
        val device = JsonHelper.jsonDecode<NearbyDevice>(
            """{"id":"device","name":"Device","port":8080,"deviceType":"FUTURE_DEVICE","version":"1","platform":"platform","lastSeen":"2026-01-01T00:00:00Z","status":"UNPAIRED","discoveryMethods":["FUTURE_METHOD"]}""",
        )
        assertEquals(DeviceType.OTHER, device.deviceType)
        assertEquals(listOf(DiscoveryMethod.UNKNOWN), device.discoveryMethods)
    }

    @Test
    fun fixed_states_decode_the_existing_wire_names() {
        for (phase in PairingPhase.entries) {
            val state = JsonHelper.jsonDecode<PairingState>(
                """{"generation":"attempt","phase":"${phase.name}"}""",
            )
            assertEquals(phase, state.phase)
        }
        for (status in AppUpdateDownloadStatus.entries) {
            val download = JsonHelper.jsonDecode<AppUpdateDownload>("""{"status":"${status.name}"}""")
            assertEquals(status, download.status)
        }
        for (mode in NotificationFilterMode.entries) {
            val filter = JsonHelper.jsonDecode<NotificationFilterData>(
                """{"mode":"${mode.name.lowercase()}","apps":[]}""",
            )
            assertEquals(mode, filter.mode)
            assertEquals(
                JsonPrimitive(mode.name.lowercase()),
                JsonHelper.jsonEncodeToElement(filter).jsonObject.getValue("mode"),
            )
        }
    }

    @Test
    fun mdns_status_decodes_without_debug_fields() {
        val snapshot = JsonHelper.jsonDecode<MdnsSnapshot>(
            """{"runtimeId":"runtime","revision":3,"running":true,"scanning":false}""",
        )
        assertEquals(true, snapshot.running)
        assertEquals(false, snapshot.scanning)
        assertEquals(emptyList(), snapshot.services)
        assertEquals(emptyList(), snapshot.packetsIn)
        assertEquals(emptyList(), snapshot.packetsOut)
    }

    @Test
    fun mdns_snapshots_preserve_addresses_packet_directions_and_large_revisions() {
        val snapshot = JsonHelper.jsonDecode<MdnsSnapshot>(
            """{"runtimeId":"runtime","revision":3000000123,"running":true,"scanning":false,"services":[{"serviceType":"service","instanceName":"instance","instanceFqdn":"instance.local","hostname":"host.local","port":8080,"txtRecords":[],"ips":["ipv4"],"ipv6":["ipv6"],"complete":true}],"packetsIn":[],"packetsOut":[{"time":3000000123,"direction":"OUT","srcIp":"source","srcPort":5353,"dstIp":"destination","dstPort":5353,"size":64,"summary":"summary","detail":"detail"}]}""",
        )
        assertEquals(3_000_000_123, snapshot.revision)
        assertEquals(listOf("ipv4"), snapshot.services.single().ips)
        assertEquals(listOf("ipv6"), snapshot.services.single().ipv6)
        assertEquals(MdnsPacketDirection.OUT, snapshot.packetsOut.single().direction)
        assertEquals(snapshot, JsonHelper.jsonDecode<MdnsSnapshot>(JsonHelper.jsonEncode(snapshot)))
    }
}
