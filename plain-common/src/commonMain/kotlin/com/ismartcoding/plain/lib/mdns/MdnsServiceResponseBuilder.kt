package com.ismartcoding.plain.lib.mdns

/**
 * Builds mDNS responses for the service published by [MdnsServiceInfo].
 *
 * A PTR query for the service type is answered with the PTR record plus A
 * records (additional), so a browser learns the instance name and its address
 * in one shot. SRV / TXT / A queries are answered with the matching records.
 */
internal object MdnsServiceResponseBuilder {

    fun buildResponseIfMatch(query: ByteArray, service: MdnsServiceInfo): MdnsServiceResponse? {
        if (service.ips.isEmpty()) return null
        val questions = MdnsPacketCodec.readQuestions(query) ?: return null
        val instanceFqdn = service.instanceFqdn

        var wantPtr = false
        var wantSrv = false
        var wantTxt = false
        var wantA = false
        for (q in questions) {
            if (q.qclass != MdnsPacketCodec.DNS_CLASS_IN) continue
            val matchesType = q.name.equals(service.serviceType, ignoreCase = true)
            val matchesInstance = q.name.equals(instanceFqdn, ignoreCase = true)
            val matchesHostname = q.name.equals(service.targetHostname, ignoreCase = true)
            when {
                q.qtype == MdnsPacketCodec.TYPE_PTR && matchesType -> wantPtr = true
                q.qtype == MdnsPacketCodec.TYPE_SRV && matchesInstance -> wantSrv = true
                q.qtype == MdnsPacketCodec.TYPE_TXT && matchesInstance -> wantTxt = true
                q.qtype == MdnsPacketCodec.TYPE_A && matchesHostname -> wantA = true
                // RFC 6762 §6: only answer ANY with records whose name matches the
                // question — otherwise we'd pollute other mDNS stacks' caches with
                // answers that have nothing to do with the queried name.
                q.qtype == MdnsPacketCodec.TYPE_ANY && (matchesType || matchesInstance || matchesHostname) -> {
                    wantPtr = wantPtr || matchesType
                    wantSrv = wantSrv || matchesInstance
                    wantTxt = wantTxt || matchesInstance
                    wantA = wantA || matchesHostname
                }
            }
        }
        if (!wantPtr && !wantSrv && !wantTxt && !wantA) return null

        val answers = mutableListOf<Byte>()
        val additional = mutableListOf<Byte>()
        if (wantPtr) answers.addAll(ptrRecord(service))
        if (wantSrv) answers.addAll(srvRecord(service))
        if (wantTxt) answers.addAll(txtRecord(service))
        if (wantA) answers.addAll(aRecords(service))
        // RFC 6763 §12: a PTR answer carries SRV/TXT/A in the additional section
        // so a single PTR query resolves the full service — the port is learned
        // in one round trip instead of a follow-up SRV query.
        var additionalCount = 0
        if (wantPtr) {
            if (!wantSrv) { additional.addAll(srvRecord(service)); additionalCount++ }
            if (!wantTxt) { additional.addAll(txtRecord(service)); additionalCount++ }
            if (!wantA) { additional.addAll(aRecords(service)); additionalCount += service.ips.size }
        } else if ((wantSrv || wantTxt) && !wantA) {
            additional.addAll(aRecords(service))
            additionalCount += service.ips.size
        }
        if (answers.isEmpty()) return null

        // Each PTR/SRV/TXT is a single record; A records are one per IP.
        val answerCount = listOf(
            if (wantPtr) 1 else 0,
            if (wantSrv) 1 else 0,
            if (wantTxt) 1 else 0,
            if (wantA) service.ips.size else 0,
        ).sum()

        val out = mutableListOf<Byte>()
        MdnsPacketCodec.writeHeader(out, answers = answerCount, additional = additionalCount)
        out.addAll(answers)
        out.addAll(additional)
        val unicast = questions.any { it.unicastResponseRequested }
        return MdnsServiceResponse(out.toByteArray(), unicast)
    }

    private fun ptrRecord(service: MdnsServiceInfo, ttl: Int = MdnsPacketCodec.TTL_SECONDS): List<Byte> {
        val out = mutableListOf<Byte>()
        MdnsPacketCodec.writeRecord(
            out,
            MdnsPacketCodec.encodeName(service.serviceType),
            MdnsPacketCodec.TYPE_PTR,
            // RFC 6762 §10.2: the cache-flush bit is only for unique records
            // (SRV/TXT/A). PTR rnames are shared by all instances of the type,
            // so flushing would evict other devices' PTR entries from peers.
            MdnsPacketCodec.DNS_CLASS_IN,
            ttl,
            MdnsPacketCodec.encodeName(service.instanceFqdn),
        )
        return out
    }

    private fun srvRecord(service: MdnsServiceInfo, ttl: Int = MdnsPacketCodec.TTL_SECONDS): List<Byte> {
        val rdata = mutableListOf<Byte>()
        MdnsPacketCodec.writeU16(rdata, 0) // priority
        MdnsPacketCodec.writeU16(rdata, 0) // weight
        MdnsPacketCodec.writeU16(rdata, service.port)
        rdata.addAll(MdnsPacketCodec.encodeName(service.targetHostname).toList())
        val out = mutableListOf<Byte>()
        MdnsPacketCodec.writeRecord(
            out,
            MdnsPacketCodec.encodeName(service.instanceFqdn),
            MdnsPacketCodec.TYPE_SRV,
            MdnsPacketCodec.DNS_CACHE_FLUSH_CLASS_IN,
            ttl,
            rdata.toByteArray(),
        )
        return out
    }

    private fun txtRecord(service: MdnsServiceInfo, ttl: Int = MdnsPacketCodec.TTL_SECONDS): List<Byte> {
        val rdata = mutableListOf<Byte>()
        service.txtRecords.forEach { value ->
            val bytes = value.encodeToByteArray()
            rdata.add(bytes.size.toByte())
            rdata.addAll(bytes.toList())
        }
        val out = mutableListOf<Byte>()
        MdnsPacketCodec.writeRecord(
            out,
            MdnsPacketCodec.encodeName(service.instanceFqdn),
            MdnsPacketCodec.TYPE_TXT,
            MdnsPacketCodec.DNS_CACHE_FLUSH_CLASS_IN,
            ttl,
            rdata.toByteArray(),
        )
        return out
    }

    /**
     * Builds the RFC 6762 §8.4 goodbye for one instance: the same PTR/SRV/TXT
     * records with TTL=0, so every resolver on the link drops the cached entry
     * at once. Needed when a published instance is replaced by one with a
     * different instance FQDN (device renamed) — without it peers keep listing
     * the old name until the 120s TTL expires.
     */
    internal fun buildGoodbye(service: MdnsServiceInfo): ByteArray {
        val out = mutableListOf<Byte>()
        MdnsPacketCodec.writeHeader(out, answers = 3, additional = 0)
        out.addAll(ptrRecord(service, ttl = 0))
        out.addAll(srvRecord(service, ttl = 0))
        out.addAll(txtRecord(service, ttl = 0))
        return out.toByteArray()
    }

    private fun aRecords(service: MdnsServiceInfo): List<Byte> {
        val nameBytes = MdnsPacketCodec.encodeName(service.targetHostname)
        val out = mutableListOf<Byte>()
        service.ips.forEach { ip ->
            MdnsPacketCodec.writeRecord(
                out,
                nameBytes,
                MdnsPacketCodec.TYPE_A,
                MdnsPacketCodec.DNS_CACHE_FLUSH_CLASS_IN,
                MdnsPacketCodec.TTL_SECONDS,
                ipToBytes(ip),
            )
        }
        return out
    }
}

internal data class MdnsServiceResponse(
    val bytes: ByteArray,
    val unicastResponseRequested: Boolean,
)
