package com.ismartcoding.plain.httpserver.mainschemas

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.helpers.QueryHelper
import com.ismartcoding.plain.platform.clearLatestLogFile
import com.ismartcoding.plain.platform.getLatestLogFilePath
import com.ismartcoding.plain.platform.readLogLinesNewestFirst

@GraphQLQuery
suspend fun appLogs(offset: Int, limit: Int, query: String): List<String> {
    val text = QueryHelper.textOf(query).trim()
    if (text.isEmpty()) return readLogLinesNewestFirst(offset, limit)
    return readLogLinesNewestFirst(0, Int.MAX_VALUE)
        .filter { it.contains(text, ignoreCase = true) }
        .drop(offset.coerceAtLeast(0))
        .take(limit.coerceAtLeast(0))
}

@GraphQLQuery
suspend fun appLogPath(): String {
    return getLatestLogFilePath()
}

@GraphQLMutation
suspend fun clearAppLogs(): Boolean {
    clearLatestLogFile()
    return true
}

fun SchemaBuilder.addAppLogsSchema() {
}
