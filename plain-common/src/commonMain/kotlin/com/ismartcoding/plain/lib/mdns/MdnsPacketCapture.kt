package com.ismartcoding.plain.lib.mdns

import kotlin.concurrent.Volatile

/**
 * Cross-platform monitor lock. `kotlin.synchronized` is JVM-only, so the
 * packet capture buffer uses this primitive (Android: `synchronized`, iOS:
 * `NSLock`) instead.
 */
internal expect fun newMdnsLock(): Any

internal expect fun <T> mdnsSynchronized(lock: Any, block: () -> T): T

/** Direction of one captured mDNS packet. */
enum class MdnsPacketDirection { IN, OUT }

/**
 * One captured mDNS datagram, decoded into a short one-line [summary] and a
 * multi-line [detail] for the debug page. [time] is ms since epoch.
 */
data class MdnsPacketLog(
    val time: Long,
    val direction: MdnsPacketDirection,
    val srcIp: String,
    val srcPort: Int,
    val dstIp: String,
    val dstPort: Int,
    val size: Int,
    val summary: String,
    val detail: String,
)

/**
 * Ring buffer of the last [CAPACITY] inbound and outbound mDNS packets for the
 * debug page. Recording is gated by [enabled]: it only turns on while the mDNS
 * debug page is open, so production overhead is a single volatile read per
 * packet. All writes are serialized under the shared responder consumer; reads
 * take an immutable snapshot.
 */
object MdnsPacketCapture {
    private const val CAPACITY = 50

    private val lock = newMdnsLock()
    private val inbound = ArrayDeque<MdnsPacketLog>()
    private val outbound = ArrayDeque<MdnsPacketLog>()

    @Volatile
    var enabled: Boolean = false
        private set

    /** Enables/disables capture; disabling also clears both buffers. */
    fun setEnabled(value: Boolean) {
        if (enabled == value) return
        mdnsSynchronized(lock) {
            enabled = value
            if (!value) {
                inbound.clear()
                outbound.clear()
            }
        }
    }

    fun recordIn(srcIp: String, srcPort: Int, bytes: ByteArray) =
        record(MdnsPacketDirection.IN, srcIp, srcPort, "", 0, bytes)

    fun recordOut(srcIp: String, srcPort: Int, dstIp: String, dstPort: Int, bytes: ByteArray) =
        record(MdnsPacketDirection.OUT, srcIp, srcPort, dstIp, dstPort, bytes)

    fun snapshotIn(): List<MdnsPacketLog> = mdnsSynchronized(lock) { inbound.toList() }

    fun snapshotOut(): List<MdnsPacketLog> = mdnsSynchronized(lock) { outbound.toList() }

    private fun record(
        direction: MdnsPacketDirection,
        srcIp: String,
        srcPort: Int,
        dstIp: String,
        dstPort: Int,
        bytes: ByteArray,
    ) {
        // Fast path with zero decoding when the debug page is closed.
        if (!enabled) return
        val decoded = decode(bytes)
        val log = MdnsPacketLog(
            time = mdnsNowMillis(),
            direction = direction,
            srcIp = srcIp,
            srcPort = srcPort,
            dstIp = dstIp,
            dstPort = dstPort,
            size = bytes.size,
            summary = decoded.first,
            detail = decoded.second,
        )
        mdnsSynchronized(lock) {
            val buffer = if (direction == MdnsPacketDirection.IN) inbound else outbound
            buffer.addFirst(log)
            while (buffer.size > CAPACITY) buffer.removeLast()
        }
    }

    /** Returns (summary, detail). Summary stays short for the list row. */
    private fun decode(bytes: ByteArray): Pair<String, String> {
        val questions = MdnsPacketCodec.readQuestions(bytes)
        val parsed = MdnsPacketCodec.parseResponse(bytes)
        val detail = buildString {
            questions?.forEach { appendLine("Q ${it.name}  ${typeName(it.qtype)}${if (it.unicastResponseRequested) " (QU)" else ""}") }
            parsed?.answers?.forEach { appendLine("ANS ${recordLine(it)}") }
            parsed?.additional?.forEach { appendLine("ADD ${recordLine(it)}") }
            if (isBlank()) append("(unparseable)")
        }.trimEnd()
        val summary = if (parsed?.isResponse == true) {
            "response  ${parsed.answers.size}ans/${parsed.additional.size}add"
        } else {
            val q = questions?.firstOrNull()
            if (q != null) "query  ${typeName(q.qtype)}  ${q.name}" else "packet"
        }
        return summary to detail
    }

    private fun recordLine(record: MdnsRecord): String {
        val name = record.name
        return when (record.type) {
            MdnsPacketCodec.TYPE_PTR -> "$name  PTR  -> ${record.ptrTarget}"
            MdnsPacketCodec.TYPE_SRV -> "$name  SRV  ${record.srv?.target ?: "?"} : ${record.srv?.port ?: 0}"
            MdnsPacketCodec.TYPE_TXT -> "$name  TXT  ${record.txtStrings?.joinToString(", ") ?: "?"}"
            MdnsPacketCodec.TYPE_A -> "$name  A  ${record.ip}"
            else -> "$name  ${typeName(record.type)}  ttl=${record.ttl}"
        }
    }

    private fun typeName(type: Int): String = when (type) {
        MdnsPacketCodec.TYPE_A -> "A"
        MdnsPacketCodec.TYPE_PTR -> "PTR"
        MdnsPacketCodec.TYPE_TXT -> "TXT"
        MdnsPacketCodec.TYPE_SRV -> "SRV"
        MdnsPacketCodec.TYPE_ANY -> "ANY"
        else -> "TYPE${type.toString(16)}"
    }
}