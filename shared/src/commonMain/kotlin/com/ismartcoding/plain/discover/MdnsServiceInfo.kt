package com.ismartcoding.plain.discover

data class MdnsServiceInfo(
    val instanceName: String,
    val serviceType: String,
    val targetHostname: String,
    val port: Int,
    val txtRecords: List<String>,
    val ips: List<String>,
) {
    val instanceFqdn: String get() = "$instanceName.$serviceType"
}
