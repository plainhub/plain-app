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
        // A records ride along as additional data for service queries.
        if ((wantPtr || wantSrv || wantTxt) && !wantA) additional.addAll(aRecords(service))
        if (answers.isEmpty()) return null

        // Each PTR/SRV/TXT is a single record; A records are one per IP.
        val answerCount = listOf(
            if (wantPtr) 1 else 0,
            if (wantSrv) 1 else 0,
            if (wantTxt) 1 else 0,
            if (wantA) service.ips.size else 0,
        ).sum()
        val additionalCount = if ((wantPtr || wantSrv || wantTxt) && !wantA) service.ips.size else 0

        val out = mutableListOf<Byte>()
        MdnsPacketCodec.writeHeader(out, answers = answerCount, additional = additionalCount)
        out.addAll(answers)
        out.addAll(additional)
        val unicast = questions.any { it.unicastResponseRequested }
        return MdnsServiceResponse(out.toByteArray(), unicast)
    }

    private fun ptrRecord(service: MdnsServiceInfo): List<Byte> {
        val out = mutableListOf<Byte>()
        MdnsPacketCodec.writeRecord(
            out,
            MdnsPacketCodec.encodeName(service.serviceType),
            MdnsPacketCodec.TYPE_PTR,
            // RFC 6762 §10.2: the cache-flush bit is only for unique records
            // (SRV/TXT/A). PTR rnames are shared by all instances of the type,
            // so flushing would evict other devices' PTR entries from peers.
            MdnsPacketCodec.DNS_CLASS_IN,
            MdnsPacketCodec.TTL_SECONDS,
            MdnsPacketCodec.encodeName(service.instanceFqdn),
        )
        return out
    }

    private fun srvRecord(service: MdnsServiceInfo): List<Byte> {
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
            MdnsPacketCodec.TTL_SECONDS,
            rdata.toByteArray(),
        )
        return out
    }

    private fun txtRecord(service: MdnsServiceInfo): List<Byte> {
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
            MdnsPacketCodec.TTL_SECONDS,
            rdata.toByteArray(),
        )
        return out
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
