package com.ismartcoding.plain.mdns

import java.io.ByteArrayOutputStream
import java.net.Inet4Address

internal object MdnsPacketCodec {
    private const val DNS_CLASS_IN = 0x0001
    private const val DNS_TYPE_A = 0x0001
    private const val DNS_TYPE_ANY = 0x00FF
    private const val DNS_RESPONSE_FLAGS = 0x8400
    private const val DNS_CACHE_FLUSH_CLASS_IN = 0x8001
    private const val TTL_SECONDS = 120

    fun buildResponseIfMatch(
        query: ByteArray,
        hostname: String,
        ips: List<Inet4Address>,
    ): ByteArray? {
        if (query.size < 12 || ips.isEmpty()) return null

        val flags = readU16(query, 2)
        // Bit 15 (QR) = 1 means this is a response, not a query. Ignore it.
        if (flags and 0x8000 != 0) return null

        val qdCount = readU16(query, 4)
        if (qdCount <= 0) return null

        var offset = 12
        var matched = false
        repeat(qdCount) {
            val parsed = readName(query, offset) ?: return null
            val qname = parsed.first
            offset = parsed.second
            if (offset + 4 > query.size) return null

            val qtype = readU16(query, offset)
            val qclass = readU16(query, offset + 2) and 0x7FFF
            offset += 4

            if (
                qname.equals(hostname, ignoreCase = true) &&
                qclass == DNS_CLASS_IN &&
                (qtype == DNS_TYPE_A || qtype == DNS_TYPE_ANY)
            ) {
                matched = true
            }
        }
        if (!matched) return null

        val nameBytes = encodeName(hostname)
        val out = ByteArrayOutputStream()
        writeU16(out, 0)
        writeU16(out, DNS_RESPONSE_FLAGS)
        writeU16(out, 0)
        writeU16(out, ips.size)
        writeU16(out, 0)
        writeU16(out, 0)

        ips.forEach { ip ->
            out.write(nameBytes)
            writeU16(out, DNS_TYPE_A)
            writeU16(out, DNS_CACHE_FLUSH_CLASS_IN)
            writeU32(out, TTL_SECONDS)
            writeU16(out, 4)
            out.write(ip.address)
        }
        return out.toByteArray()
    }

    private fun encodeName(name: String): ByteArray {
        val out = ByteArrayOutputStream()
        name.split('.')
            .filter { it.isNotEmpty() }
            .forEach { label ->
                val bytes = label.toByteArray(Charsets.UTF_8)
                out.write(bytes.size)
                out.write(bytes)
            }
        out.write(0)
        return out.toByteArray()
    }

    private fun readName(data: ByteArray, start: Int, depth: Int = 0): Pair<String, Int>? {
        if (depth > 8 || start >= data.size) return null

        val labels = mutableListOf<String>()
        var offset = start
        while (offset < data.size) {
            val len = data[offset].toInt() and 0xFF
            if (len == 0) return Pair(labels.joinToString("."), offset + 1)

            if ((len and 0xC0) == 0xC0) {
                if (offset + 1 >= data.size) return null
                val ptr = ((len and 0x3F) shl 8) or (data[offset + 1].toInt() and 0xFF)
                val pointed = readName(data, ptr, depth + 1) ?: return null
                val pointedLabels = pointed.first.split('.').filter { it.isNotEmpty() }
                return Pair((labels + pointedLabels).joinToString("."), offset + 2)
            }

            val next = offset + 1 + len
            if (next > data.size) return null
            labels.add(String(data, offset + 1, len, Charsets.UTF_8))
            offset = next
        }
        return null
    }

    private fun readU16(data: ByteArray, offset: Int): Int {
        return ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
    }

    private fun writeU16(out: ByteArrayOutputStream, value: Int) {
        out.write((value ushr 8) and 0xFF)
        out.write(value and 0xFF)
    }

    private fun writeU32(out: ByteArrayOutputStream, value: Int) {
        out.write((value ushr 24) and 0xFF)
        out.write((value ushr 16) and 0xFF)
        out.write((value ushr 8) and 0xFF)
        out.write(value and 0xFF)
    }
}
