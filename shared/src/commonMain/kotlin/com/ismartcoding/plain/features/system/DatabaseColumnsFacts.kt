package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
internal data class DatabaseColumnsFacts(
    val columns: List<com.ismartcoding.plain.httpserver.models.DbTableColumn>,
)
