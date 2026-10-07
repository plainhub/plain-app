package com.ismartcoding.plain.httpserver.models

import kotlinx.serialization.Serializable

@Serializable
data class DbTableColumn(
    val name: String,
    val dataType: DbColumnType,
    val notNull: Boolean,
    val defaultValue: String?,
    val primaryKey: Boolean,
)
