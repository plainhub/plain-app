package com.ismartcoding.plain.platform

import com.ismartcoding.plain.httpserver.models.DbTableInfo

actual fun getDbPath(): String = ""

actual suspend fun getDbTableNames(): List<String> = emptyList()

actual suspend fun getDbTableRowCount(table: String): Long = 0L

actual suspend fun getDbTableRows(table: String, offset: Int, limit: Int): List<String> = emptyList()

actual suspend fun getDbTableInfo(table: String): DbTableInfo = DbTableInfo(idKey = "id")

actual suspend fun createDbTableRow(table: String, rowJson: String): Boolean = false

actual suspend fun deleteDbTableRows(table: String, ids: List<String>): Boolean = false
