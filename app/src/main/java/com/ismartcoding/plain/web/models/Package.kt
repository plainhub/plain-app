package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.features.pkg.DPackage
import kotlinx.datetime.Instant

data class Package(
    val id: ID,
    val name: String,
    val type: String,
    val version: String,
    val path: String,
    val size: Long,
    val installedAt: Instant,
    val updatedAt: Instant
)

fun DPackage.toModel(): Package {
    return Package(ID(id), name, type, version, path, size, installedAt, updatedAt)
}


data class PackageStatus(val id: ID, val exist: Boolean)