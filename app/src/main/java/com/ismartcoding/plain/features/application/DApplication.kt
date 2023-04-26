package com.ismartcoding.plain.features.application

import kotlinx.datetime.Instant

data class DApplication(
    val id: String,
    val name: String,
    val type: String,
    val version: String,
    val path: String,
    val size: Long,
    val installedAt: Instant,
    val updatedAt: Instant
)