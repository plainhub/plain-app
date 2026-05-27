package com.ismartcoding.plain.web.schemas

import com.ismartcoding.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.lib.logcat.DiskLogFormatStrategy
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.helpers.AppLogHelper
import java.io.File

fun SchemaBuilder.addAppLogsSchema() {
    query("appLogs") {
        resolver { offset: Int, limit: Int ->
            val context = MainApp.instance
            val logFile = File(DiskLogFormatStrategy.getLogFolder(context), "latest.log")
            AppLogHelper.getLogLines(logFile, offset, limit)
        }
    }

    query("appLogPath") {
        resolver { ->
            val context = MainApp.instance
            DiskLogFormatStrategy.getLogFolder(context) + "/latest.log"
        }
    }

    mutation("clearAppLogs") {
        resolver { ->
            val context = MainApp.instance
            val logFile = File(DiskLogFormatStrategy.getLogFolder(context), "latest.log")
            if (logFile.exists()) logFile.writeText("")
            true
        }
    }
}
