package com.ismartcoding.plain.features.pkg

import kotlinx.datetime.Instant

data class DPackage(
    val id: String,
    val name: String,
    val type: String,
    val version: String,
    val path: String,
    val size: Long,
    val installedAt: Instant,
    val updatedAt: Instant
)