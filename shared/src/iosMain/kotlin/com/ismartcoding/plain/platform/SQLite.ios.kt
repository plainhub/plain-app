package com.ismartcoding.plain.platform

import com.ismartcoding.plain.web.models.DbTableInfo

actual fun getDbPath(): String = ""

actual fun getDbTableNames(): List<String> = emptyList()

actual fun getDbTableRowCount(table: String): Long = 0L

actual fun getDbTableRows(table: String, offset: Int, limit: Int): List<String> = emptyList()

actual fun getDbTableInfo(table: String): DbTableInfo = DbTableInfo(idKey = "id")

actual fun createDbTableRow(table: String, rowJson: String): Boolean = false

actual fun deleteDbTableRows(table: String, ids: List<String>): Boolean = false
