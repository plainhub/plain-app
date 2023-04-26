package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.features.application.DApplication
import kotlinx.datetime.Instant

data class Application(
    val id: ID,
    val name: String,
    val type: String,
    val version: String,
    val path: String,
    val size: Long,
    val installedAt: Instant,
    val updatedAt: Instant
)

fun DApplication.toModel(): Application {
    return Application(ID(id), name, type, version, path, size, installedAt, updatedAt)
}
