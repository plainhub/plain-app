package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import kotlin.time.Instant

@GraphQLType
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

@GraphQLType
data class Certificate(val issuer: String, val subject: String, val serialNumber: String, val validFrom: Instant, val validTo: Instant)

@GraphQLType
data class PackageStatus(val id: ID, val exist: Boolean, val updatedAt: Instant?)
