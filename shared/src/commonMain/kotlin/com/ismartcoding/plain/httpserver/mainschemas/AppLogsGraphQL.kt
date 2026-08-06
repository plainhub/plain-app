package com.ismartcoding.plain.httpserver.mainschemas

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.platform.clearLatestLogFile
import com.ismartcoding.plain.platform.getLatestLogFilePath
import com.ismartcoding.plain.platform.readLogLinesNewestFirst

@GraphQLQuery
suspend fun appLogs(offset: Int, limit: Int): List<String> {
    return readLogLinesNewestFirst(offset, limit)
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
