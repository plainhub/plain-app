package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
internal data class DatabaseFacts(
    val path: String,
    val tables: List<String>,
)
