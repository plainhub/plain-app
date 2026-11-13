package com.ismartcoding.plain.models

import com.ismartcoding.plain.features.sessions.SessionType
import com.ismartcoding.plain.features.images.search.ImageSearchStatusType
import com.ismartcoding.plain.features.feeds.FeedWorkerStatus
import com.ismartcoding.plain.features.nearby.DiscoveryMethod
import com.ismartcoding.plain.features.nearby.MdnsPacketDirection
import com.ismartcoding.plain.features.nearby.MdnsSnapshot
import com.ismartcoding.plain.features.nearby.MdnsPacketLog
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
import kotlin.time.Instant
import kotlinx.serialization.SerializationException
import kotlin.test.assertFailsWith
import kotlin.test.assertEquals

class ClientContractModelsTest {
    @Test
    fun tag_types_preserve_the_graphql_enum_name() {
        val tag = JsonHelper.jsonDecode<Tag>("""{"id":"tag","name":"Label","count":1,"type":"NOTE"}""")
        assertEquals(DataType.NOTE, tag.type)
        assertEquals(JsonPrimitive("NOTE"), JsonHelper.jsonEncodeToElement(tag).jsonObject.getValue("type"))
    }

    @Test
    fun nearby_devices_decode_every_supported_discovery_method() {
        val device = JsonHelper.jsonDecode<NearbyDevice>(
            """{"id":"device","signaturePublicKey":"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=","name":"Device","port":8080,"deviceType":"PHONE","version":"1","platform":"android","lastSeen":"2026-01-01T00:00:00Z","status":"UNPAIRED","discoveryMethods":["LAN","BLE","QR"]}""",
        )
        assertEquals(DeviceType.PHONE, device.deviceType)
        assertEquals(Instant.parse("2026-01-01T00:00:00Z"), device.lastSeen)
        assertEquals(PeerStatus.UNPAIRED, device.status)
        assertEquals(
            listOf(DiscoveryMethod.LAN, DiscoveryMethod.BLE, DiscoveryMethod.QR),
            device.discoveryMethods,
        )
    }

    @Test
    fun nearby_discovery_methods_reject_unknown_values() {
        assertFailsWith<SerializationException> {
            JsonHelper.jsonDecode<NearbyDevice>(
                """{"id":"device","signaturePublicKey":"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=","name":"Device","port":8080,"deviceType":"OTHER","version":"1","platform":"platform","lastSeen":"2026-01-01T00:00:00Z","status":"UNPAIRED","discoveryMethods":["FUTURE_METHOD"]}""",
            )
        }
    }

    @Test
    fun nearby_device_types_use_the_enum_contract() {
        val payload = """{"id":"device","signaturePublicKey":"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=","name":"Device","port":8080,"deviceType":"%s","version":"1","platform":"platform","lastSeen":"2026-01-01T00:00:00Z","status":"UNPAIRED"}"""
        for (type in DeviceType.entries) {
            val device = JsonHelper.jsonDecode<NearbyDevice>(payload.replace("%s", type.name))
            assertEquals(type, device.deviceType)
            assertEquals(device, JsonHelper.jsonDecode<NearbyDevice>(JsonHelper.jsonEncode(device)))
        }
        assertFailsWith<SerializationException> {
            JsonHelper.jsonDecode<NearbyDevice>(payload.replace("%s", "FUTURE_DEVICE"))
        }
    }

    @Test
    fun fixed_enum_values_reject_unknown_names() {
        val unknown = "\"FUTURE_VALUE\""
        assertFailsWith<SerializationException> { JsonHelper.jsonDecode<PeerStatus>(unknown) }
        assertFailsWith<SerializationException> { JsonHelper.jsonDecode<DiscoveryMethod>(unknown) }
        assertFailsWith<SerializationException> { JsonHelper.jsonDecode<MdnsPacketDirection>(unknown) }
        assertFailsWith<SerializationException> { JsonHelper.jsonDecode<SessionType>(unknown) }
        assertFailsWith<SerializationException> { JsonHelper.jsonDecode<ImageSearchStatusType>(unknown) }
        assertFailsWith<SerializationException> { JsonHelper.jsonDecode<FeedWorkerStatus>(unknown) }
        assertEquals(listOf("LAN", "BLE", "QR"), DiscoveryMethod.entries.map { it.name })

    }

    @Test
    fun fixed_states_decode_the_existing_wire_names() {
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
            """{"runtimeId":"runtime","revision":3000000123,"running":true,"scanning":false,"services":[{"serviceType":"service","instanceName":"instance","instanceFqdn":"instance.local","hostname":"host.local","port":8080,"txtRecords":[],"ips":["ipv4"],"ip6s":["ipv6"],"complete":true}],"packetsIn":[],"packetsOut":[{"time":"2026-11-12T08:09:10.123Z","direction":"OUT","srcIp":"source","srcPort":5353,"dstIp":"destination","dstPort":5353,"size":64,"summary":"summary","detail":"detail"}]}""",
        )
        assertEquals(3_000_000_123, snapshot.revision)
        assertEquals(listOf("ipv4"), snapshot.services.single().ips)
        assertEquals(listOf("ipv6"), snapshot.services.single().ip6s)
        assertEquals(MdnsPacketDirection.OUT, snapshot.packetsOut.single().direction)
        assertEquals(Instant.parse("2026-11-12T08:09:10.123Z"), snapshot.packetsOut.single().time)
        assertEquals(snapshot, JsonHelper.jsonDecode<MdnsSnapshot>(JsonHelper.jsonEncode(snapshot)))
    }
    @Test
    fun mdns_packet_time_requires_an_instant() {
        val payload = """{"time":1794470950123,"direction":"OUT","srcIp":"source","srcPort":5353,"dstIp":"destination","dstPort":5353,"size":64,"summary":"summary","detail":"detail"}"""
        assertFailsWith<SerializationException> { JsonHelper.jsonDecode<MdnsPacketLog>(payload) }
    }

    @Test
    fun share_expiry_and_typed_paths_decode_without_public_credentials() {
        val payload = """{"id":"share","name":"Example","password":"","readOnly":true,"expiresAt":"2026-11-12T08:09:10.123Z","data":[{"virtualPath":"/alias","realPath":"/owned/folder","isDir":true}],"url":"https://device.test/s/share#key"}"""
        val info = JsonHelper.jsonDecode<Share>(payload)
        assertEquals(Instant.parse("2026-11-12T08:09:10.123Z"), info.expiresAt)
        assertEquals(SharePath("/alias", "/owned/folder", true), info.data.single())
        assertEquals(null, JsonHelper.jsonDecode<Share>(payload.replace("\"2026-11-12T08:09:10.123Z\"", "null")).expiresAt)
    }

}
