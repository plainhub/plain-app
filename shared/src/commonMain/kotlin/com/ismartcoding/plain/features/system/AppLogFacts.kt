package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
internal data class AppLogFacts(
    val path: String,
    val lines: List<String>,
)
