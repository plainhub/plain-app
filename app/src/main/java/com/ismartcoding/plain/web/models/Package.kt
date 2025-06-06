package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.data.DPackage
import kotlinx.datetime.Instant

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

fun DPackage.toModel(): Package {
    return Package(
        ID(id), name, type, version, path, size,
        certs.map { Certificate(it.issuer, it.subject, it.serialNumber, it.validFrom, it.validTo) },
        installedAt, updatedAt,
    )
}

data class PackageStatus(val id: ID, val exist: Boolean, val updatedAt: Instant?)
