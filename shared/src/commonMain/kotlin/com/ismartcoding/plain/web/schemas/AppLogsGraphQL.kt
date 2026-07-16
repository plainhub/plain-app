package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.platform.clearLatestLogFile
import com.ismartcoding.plain.platform.getLatestLogFilePath
import com.ismartcoding.plain.platform.readLogLinesNewestFirst

fun SchemaBuilder.addAppLogsSchema() {
    query("appLogs") {
        resolver("offset", "limit") { offset: Int, limit: Int ->
            readLogLinesNewestFirst(offset, limit)
        }
    }

    query("appLogPath") {
        resolver { ->
            getLatestLogFilePath()
        }
    }

    mutation("clearAppLogs") {
        resolver { ->
            clearLatestLogFile()
            true
        }
    }
}
