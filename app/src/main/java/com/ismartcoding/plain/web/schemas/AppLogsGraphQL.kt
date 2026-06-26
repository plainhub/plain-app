package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.lib.logcat.DiskLogFormatStrategy
import com.ismartcoding.plain.helpers.AppLogHelper
import java.io.File

fun SchemaBuilder.addAppLogsSchema() {
    query("appLogs") {
        resolver { offset: Int, limit: Int ->
            val logFile = File(DiskLogFormatStrategy.getLogFolder(), "latest.log")
            AppLogHelper.getLogLines(logFile, offset, limit)
        }
    }

    query("appLogPath") {
        resolver { ->
            DiskLogFormatStrategy.getLogFolder() + "/latest.log"
        }
    }

    mutation("clearAppLogs") {
        resolver { ->
            val logFile = File(DiskLogFormatStrategy.getLogFolder(), "latest.log")
            if (logFile.exists()) logFile.writeText("")
            true
        }
    }
}
