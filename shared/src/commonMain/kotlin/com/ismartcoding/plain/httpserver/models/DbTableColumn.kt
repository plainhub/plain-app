package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType

@GraphQLType
data class DbTableColumn(
    val name: String,
    val dataType: DbColumnType,
    val notNull: Boolean,
    val defaultValue: String?,
    val primaryKey: Boolean,
)
