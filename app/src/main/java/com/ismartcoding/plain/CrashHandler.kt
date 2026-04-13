package com.ismartcoding.plain

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object CrashHandler {
    private const val CRASH_FILE_NAME = "crash_report.txt"
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun install(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            saveCrash(context, throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun getPendingReport(context: Context): String? {
        val file = File(context.filesDir, CRASH_FILE_NAME)
        if (!file.exists()) return null
        return try {
            val content = file.readText()
            file.delete()
            content
        } catch (_: Exception) {
            null
        }
    }

    fun getAppLogs(context: Context): String {
        val logFile = File(context.filesDir, "logs/latest.log")
        if (!logFile.exists()) return ""
        return try {
            val lines = logFile.readLines()
            lines.takeLast(200).joinToString("\n")
        } catch (_: Exception) {
            ""
        }
    }

    private fun saveCrash(context: Context, throwable: Throwable) {
        try {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val timestamp = dateFormat.format(LocalDateTime.now())
            val report = buildString {
                append("Time: $timestamp\n")
                append(sw.toString())
            }
            File(context.filesDir, CRASH_FILE_NAME).writeText(report)
        } catch (_: Exception) {}
    }
}
