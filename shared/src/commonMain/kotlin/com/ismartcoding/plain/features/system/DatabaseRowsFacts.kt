package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
internal data class DatabaseRowsFacts(
    val rows: List<String>,
)
