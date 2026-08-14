package com.ismartcoding.plain.mdns

/**
 * DNS/mDNS wire-format codec shared by the hostname responder, the service
 * publisher and the service browser.
 *
 * Covers:
 *  - A-record query matching / response building (hostname responder)
 *  - PTR/SRV/TXT/A query building (browser)
 *  - DNS message parsing into typed records (browser)
 */
internal object MdnsPacketCodec {
    const val DNS_CLASS_IN = 0x0001
    const val TYPE_A = 0x0001
    const val TYPE_PTR = 0x000C
    const val TYPE_TXT = 0x0010
    const val TYPE_SRV = 0x0021
    const val TYPE_ANY = 0x00FF
    const val DNS_RESPONSE_FLAGS = 0x8400
    const val DNS_CACHE_FLUSH_CLASS_IN = 0x8001
    const val TTL_SECONDS = 120

    // ---- A-record responder (hostname) -------------------------------------

    fun buildResponseIfMatch(
        query: ByteArray,
        hostname: String,
        ips: List<String>,
    ): ByteArray? = buildResponseIfMatchDetails(query, hostname, ips)?.bytes

    fun buildResponseIfMatchDetails(
        query: ByteArray,
        hostname: String,
        ips: List<String>,
    ): MdnsResponse? {
        if (ips.isEmpty()) return null
        val questions = readQuestions(query) ?: return null
        val matchedQuestions = questions.filter {
            it.name.equals(hostname, ignoreCase = true) &&
                it.qclass == DNS_CLASS_IN &&
                (it.qtype == TYPE_A || it.qtype == TYPE_ANY)
        }
        if (matchedQuestions.isEmpty()) return null

        val nameBytes = encodeName(hostname)
        val out = mutableListOf<Byte>()
        writeHeader(out, answers = ips.size, additional = 0)
        ips.forEach { ip ->
            writeRecord(out, nameBytes, TYPE_A, DNS_CACHE_FLUSH_CLASS_IN, TTL_SECONDS, ipToBytes(ip))
        }
        return MdnsResponse(out.toByteArray(), questions, matchedQuestions)
    }

    // ---- Query builders ------------------------------------------------------

    fun buildQuery(name: String, qtype: Int, unicastResponse: Boolean = false): ByteArray {
        val out = mutableListOf<Byte>()
        writeU16(out, 0) // ID
        writeU16(out, 0) // flags: query
        writeU16(out, 1) // QDCOUNT
        writeU16(out, 0) // ANCOUNT
        writeU16(out, 0) // NSCOUNT
        writeU16(out, 0) // ARCOUNT
        out.addAll(encodeName(name).toList())
        writeU16(out, qtype)
        writeU16(out, if (unicastResponse) 0x8001 else DNS_CLASS_IN)
        return out.toByteArray()
    }

    fun buildPtrQuery(serviceType: String): ByteArray = buildQuery(serviceType, TYPE_PTR)

    fun buildSrvQuery(instanceName: String, serviceType: String): ByteArray =
        buildQuery("$instanceName.$serviceType", TYPE_SRV)

    fun buildTxtQuery(instanceName: String, serviceType: String): ByteArray =
        buildQuery("$instanceName.$serviceType", TYPE_TXT)

    // ---- Response parsing -----------------------------------------------------

    /**
     * Parses a DNS/mDNS message into its answers and additional records.
     * The query section (if present) is skipped.
     */
    fun parseResponse(data: ByteArray): MdnsParsedResponse? {
        if (data.size < 12) return null
        val flags = readU16(data, 2)
        val qdCount = readU16(data, 4)
        val anCount = readU16(data, 6)
        val nsCount = readU16(data, 8)
        val arCount = readU16(data, 10)

        var offset = 12
        repeat(qdCount) {
            val parsed = readName(data, offset) ?: return null
            offset = parsed.second + 4
            if (offset > data.size) return null
        }
        val answers = readRecords(data, offset, anCount) ?: return null
        offset = answers.second
        val authority = readRecords(data, offset, nsCount) ?: return null
        offset = authority.second
        val additional = readRecords(data, offset, arCount) ?: return null
        return MdnsParsedResponse(flags, answers.first, additional.first)
    }

    private fun readRecords(data: ByteArray, start: Int, count: Int): Pair<List<MdnsRecord>, Int>? {
        val records = mutableListOf<MdnsRecord>()
        var offset = start
        repeat(count) {
            val nameResult = readName(data, offset) ?: return null
            val name = nameResult.first
            offset = nameResult.second
            if (offset + 10 > data.size) return null
            val type = readU16(data, offset)
            val cls = readU16(data, offset + 2)
            val ttl = readU32(data, offset + 4)
            val rdlen = readU16(data, offset + 8)
            offset += 10
            if (offset + rdlen > data.size) return null
            records.add(MdnsRecord(name, type, cls, ttl, data, offset, rdlen))
            offset += rdlen
        }
        return records to offset
    }

    /** Parses the question section of a query message. Null if not a query. */
    fun readQuestions(data: ByteArray): List<MdnsQuestion>? {
        if (data.size < 12) return null
        // Bit 15 (QR) = 1 means this is a response, not a query. Ignore it.
        if (readU16(data, 2) and 0x8000 != 0) return null
        val qdCount = readU16(data, 4)
        if (qdCount <= 0) return null

        var offset = 12
        val questions = mutableListOf<MdnsQuestion>()
        repeat(qdCount) {
            val parsed = readName(data, offset) ?: return null
            val qname = parsed.first
            offset = parsed.second
            if (offset + 4 > data.size) return null

            val qtype = readU16(data, offset)
            val qclassRaw = readU16(data, offset + 2)
            questions.add(
                MdnsQuestion(
                    name = qname,
                    qtype = qtype,
                    qclass = qclassRaw and 0x7FFF,
                    unicastResponseRequested = qclassRaw and 0x8000 != 0,
                )
            )
            offset += 4
        }
        return questions
    }

    // ---- DNS wire-format helpers ----------------------------------------------

    internal fun writeHeader(out: MutableList<Byte>, answers: Int, additional: Int) {
        writeU16(out, 0)
        writeU16(out, DNS_RESPONSE_FLAGS)
        writeU16(out, 0)
        writeU16(out, answers)
        writeU16(out, 0)
        writeU16(out, additional)
    }

    internal fun writeRecord(
        out: MutableList<Byte>,
        name: ByteArray,
        type: Int,
        cls: Int,
        ttl: Int,
        rdata: ByteArray,
    ) {
        out.addAll(name.toList())
        writeU16(out, type)
        writeU16(out, cls)
        writeU32(out, ttl)
        writeU16(out, rdata.size)
        out.addAll(rdata.toList())
    }

    internal fun encodeName(name: String): ByteArray {
        val out = mutableListOf<Byte>()
        name.split('.')
            .filter { it.isNotEmpty() }
            .forEach { label ->
                val bytes = label.encodeToByteArray()
                out.add(bytes.size.toByte())
                out.addAll(bytes.toList())
            }
        out.add(0)
        return out.toByteArray()
    }

    internal fun readName(data: ByteArray, start: Int, depth: Int = 0): Pair<String, Int>? {
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
            labels.add(data.copyOfRange(offset + 1, offset + 1 + len).decodeToString())
            offset = next
        }
        return null
    }

    internal fun readU16(data: ByteArray, offset: Int): Int =
        ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)

    internal fun readU32(data: ByteArray, offset: Int): Long =
        ((data[offset].toLong() and 0xFF) shl 24) or
            ((data[offset + 1].toLong() and 0xFF) shl 16) or
            ((data[offset + 2].toLong() and 0xFF) shl 8) or
            (data[offset + 3].toLong() and 0xFF)

    internal fun writeU16(out: MutableList<Byte>, value: Int) {
        out.add(((value ushr 8) and 0xFF).toByte())
        out.add((value and 0xFF).toByte())
    }

    internal fun writeU32(out: MutableList<Byte>, value: Int) {
        out.add(((value ushr 24) and 0xFF).toByte())
        out.add(((value ushr 16) and 0xFF).toByte())
        out.add(((value ushr 8) and 0xFF).toByte())
        out.add((value and 0xFF).toByte())
    }
}

internal data class MdnsQuestion(
    val name: String,
    val qtype: Int,
    val qclass: Int,
    val unicastResponseRequested: Boolean,
)

internal data class MdnsResponse(
    val bytes: ByteArray,
    val questions: List<MdnsQuestion>,
    val matchedQuestions: List<MdnsQuestion>,
) {
    val unicastResponseRequested: Boolean
        get() = matchedQuestions.any { it.unicastResponseRequested }
}
