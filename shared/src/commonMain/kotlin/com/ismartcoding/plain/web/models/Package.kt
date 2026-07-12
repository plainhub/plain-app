package com.ismartcoding.plain.web.models

import kotlin.time.Instant

data class Package(
    val id: ID,
    val name: String,
    val type: String,
    val version: String,
    val path: String,
    val size: Long,
    val certs: List<Certificate>,
    val installedAt: Instant,
    val updatedAt: Instant,
)

data class Certificate(val issuer: String, val subject: String, val serialNumber: String, val validFrom: Instant, val validTo: Instant)

data class PackageStatus(val id: ID, val exist: Boolean, val updatedAt: Instant?)
