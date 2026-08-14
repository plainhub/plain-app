package com.ismartcoding.plain.mdns

import com.ismartcoding.plain.discover.DDiscoverReply

/** mDNS service type advertised by PlainApp devices. */
internal const val PLAINAPP_SERVICE_TYPE = "_plainapp._tcp.local"

/** A service instance published by a PlainApp device over mDNS. */
internal data class MdnsServiceInfo(
    val instanceName: String,   // e.g. "Pixel 7 Pro"
    val serviceType: String,    // e.g. "_plainapp._tcp.local"
    val targetHostname: String, // e.g. "plainapp-abc123.local"
    val port: Int,
    val txtRecords: List<String>, // e.g. ["id=abc123", "dv=PHONE"]
    val ips: List<String>,
) {
    val instanceFqdn: String get() = "$instanceName.$serviceType"
}

/**
 * Read-only mDNS details for one discovered device, exposed by
 * [com.ismartcoding.plain.mdns.MdnsServiceBrowser.snapshot] for the mDNS debug
 * page ([com.ismartcoding.plain.ui.page.settings.MdnsDebugPage]). Shows the raw
 * wire data exactly as parsed (service type, instance, hostname, port, TXT
 * records) so the protocol implementation can be checked against RFC 6762.
 */
internal data class MdnsServiceSnapshot(
    val serviceType: String,
    val instanceName: String,
    val instanceFqdn: String,
    val hostname: String,
    val port: Int,
    val txtRecords: List<String>,
    val ips: List<String>,
    val complete: Boolean,
)

/** Parsed SRV record payload. */
internal data class MdnsSrvRecord(
    val priority: Int,
    val weight: Int,
    val port: Int,
    val target: String,
)

/**
 * Builds the advertised mDNS service for this device from a discovery reply.
 * TXT keys mirror [DDiscoverReply] fields (see design doc §4.1).
 */
internal fun buildMdnsServiceInfo(reply: DDiscoverReply, hostname: String): MdnsServiceInfo =
    MdnsServiceInfo(
        instanceName = reply.name,
        serviceType = PLAINAPP_SERVICE_TYPE,
        targetHostname = hostname,
        port = reply.port,
        txtRecords = listOf(
            "id=${reply.id}",
            "dv=${reply.deviceType.name}",
            "ver=${reply.version}",
            "pf=${reply.platform}",
            "aw=${if (reply.awareSupported) "1" else "0"}",
            "ar=${if (reply.awareRunning) "1" else "0"}",
        ),
        ips = reply.ips,
    )

/** One resource record parsed from a DNS/mDNS message. */
internal data class MdnsRecord(
    val name: String,
    val type: Int,
    val cls: Int,
    val ttl: Long,
    val packet: ByteArray,
    val rdataStart: Int,
    val rdataLength: Int,
) {
    val cacheFlush: Boolean get() = cls and 0x8000 != 0
    val dnsClass: Int get() = cls and 0x7FFF

    /** PTR RDATA — the target instance FQDN. */
    val ptrTarget: String?
        get() = if (type == MdnsPacketCodec.TYPE_PTR) {
            MdnsPacketCodec.readName(packet, rdataStart)?.first
        } else null

    /** SRV RDATA — priority/weight/port/target. */
    val srv: MdnsSrvRecord?
        get() = if (type == MdnsPacketCodec.TYPE_SRV && rdataLength >= 6) {
            MdnsSrvRecord(
                priority = MdnsPacketCodec.readU16(packet, rdataStart),
                weight = MdnsPacketCodec.readU16(packet, rdataStart + 2),
                port = MdnsPacketCodec.readU16(packet, rdataStart + 4),
                target = MdnsPacketCodec.readName(packet, rdataStart + 6)?.first ?: "",
            )
        } else null

    /** TXT RDATA — list of "key=value" strings. */
    val txtStrings: List<String>?
        get() = if (type == MdnsPacketCodec.TYPE_TXT) parseTxtStrings() else null

    /** A RDATA — IPv4 dotted-quad string. */
    val ip: String?
        get() = if (type == MdnsPacketCodec.TYPE_A && rdataLength == 4) {
            buildString {
                for (i in 0 until 4) {
                    if (i > 0) append('.')
                    append(packet[rdataStart + i].toInt() and 0xFF)
                }
            }
        } else null

    private fun parseTxtStrings(): List<String> {
        val strings = mutableListOf<String>()
        var offset = rdataStart
        val end = rdataStart + rdataLength
        while (offset < end) {
            val len = packet[offset].toInt() and 0xFF
            offset += 1
            if (offset + len > end) break
            strings.add(packet.copyOfRange(offset, offset + len).decodeToString())
            offset += len
        }
        return strings
    }
}

/** Parsed DNS/mDNS message: header flags plus answer/additional records. */
internal data class MdnsParsedResponse(
    val flags: Int,
    val answers: List<MdnsRecord>,
    val additional: List<MdnsRecord>,
) {
    val isResponse: Boolean get() = flags and 0x8000 != 0
    val allRecords: List<MdnsRecord> get() = answers + additional
}
