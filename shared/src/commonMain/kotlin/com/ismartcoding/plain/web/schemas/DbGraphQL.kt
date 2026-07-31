package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.platform.createDbTableRow
import com.ismartcoding.plain.platform.deleteDbTableRows
import com.ismartcoding.plain.platform.getDbPath
import com.ismartcoding.plain.platform.getDbTableInfo
import com.ismartcoding.plain.platform.getDbTableNames
import com.ismartcoding.plain.platform.getDbTableRowCount
import com.ismartcoding.plain.platform.getDbTableRows
import com.ismartcoding.plain.web.models.DbTableInfo

@GraphQLQuery
suspend fun dbPath(): String {
    return getDbPath()
}

@GraphQLQuery
suspend fun dbTables(): List<String> {
    return getDbTableNames()
}

@GraphQLQuery
suspend fun dbTableRowCount(table: String): Long {
    return getDbTableRowCount(table)
}

@GraphQLQuery
suspend fun dbTableRows(table: String, offset: Int, limit: Int): List<String> {
    return getDbTableRows(table, offset, limit)
}

@GraphQLQuery
suspend fun dbTableInfo(table: String): DbTableInfo {
    return getDbTableInfo(table)
}

@GraphQLMutation
suspend fun createDbTableRow(table: String, row: String): Boolean {
    return com.ismartcoding.plain.platform.createDbTableRow(table, row)
}

@GraphQLMutation
suspend fun deleteDbTableRows(table: String, ids: List<String>): Boolean {
    return com.ismartcoding.plain.platform.deleteDbTableRows(table, ids)
}

fun SchemaBuilder.addDbSchema() {
}
