package com.ismartcoding.plain.tests

import com.ismartcoding.plain.mdns.MdnsPacketCodec
import com.ismartcoding.plain.mdns.MdnsServiceInfo
import com.ismartcoding.plain.mdns.MdnsServiceResponseBuilder
import org.junit.Test

class MdnsCodecDebugTest {
    @Test
    fun debug() {
        val service = MdnsServiceInfo(
            instanceName = "Pixel 7 Pro",
            serviceType = "_plainapp._tcp.local",
            targetHostname = "plainapp-abc123.local",
            port = 8443,
            txtRecords = listOf("id=abc123"),
            ips = listOf("192.168.1.50"),
        )
        val query = MdnsPacketCodec.buildPtrQuery(service.serviceType)
        println("QUERY bytes: ${query.size} ${query.joinToString(",") { (it.toInt() and 0xFF).toString() }}")
        val response = MdnsServiceResponseBuilder.buildResponseIfMatch(query, service)!!
        println("RESP bytes: ${response.bytes.size}")
        println("RESP hex: ${response.bytes.joinToString(",") { (it.toInt() and 0xFF).toString() }}")
        val parsed = MdnsPacketCodec.parseResponse(response.bytes)
        println("PARSED: $parsed")
        if (parsed != null) {
            println("answers=${parsed.answers.size} additional=${parsed.additional.size}")
            parsed.answers.forEach { println("A name=${it.name} type=${it.type}") }
            parsed.additional.forEach { println("ADD name=${it.name} type=${it.type} ip=${it.ip}") }
        }
        println("questions: ${MdnsPacketCodec.readQuestions(query)}")
    }
}
