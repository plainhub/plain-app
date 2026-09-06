package com.ismartcoding.plain.platform

import androidx.room3.useReaderConnection
import androidx.room3.useWriterConnection
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.httpserver.models.DbTableInfo
import org.json.JSONObject

private val ALLOWED_NAME_REGEX = Regex("^[a-zA-Z_][a-zA-Z0-9_]*$")

private fun requireSafeName(name: String) {
    require(ALLOWED_NAME_REGEX.matches(name)) { "Invalid identifier: $name" }
}

private suspend fun getValidatedTableName(table: String): String {
    requireSafeName(table)
    val exists = AppDatabase.instance.useReaderConnection { c ->
        c.usePrepared("SELECT name FROM sqlite_master WHERE type='table' AND name=?") {
            it.bindText(1, table)
            it.step()
        }
    }
    require(exists) { "Table not found: $table" }
    return table
}

private suspend fun primaryKeyColumn(table: String): String {
    val safeName = getValidatedTableName(table)
    return AppDatabase.instance.useReaderConnection { c ->
        c.usePrepared("PRAGMA table_info(`$safeName`)") { stmt ->
            var result = "id"
            while (stmt.step()) {
                if (stmt.getLong(5) > 0) {
                    val colName = stmt.getText(1)
                    requireSafeName(colName)
                    result = colName
                    break
                }
            }
            result
        }
    }
}

actual fun getDbPath(): String =
    appContext.getDatabasePath(Constants.DATABASE_NAME).absolutePath

actual suspend fun getDbTableNames(): List<String> = AppDatabase.instance.useReaderConnection { c ->
    c.usePrepared(
        "SELECT name FROM sqlite_master WHERE type='table'" +
            " AND name NOT LIKE 'android_%' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'room_%'" +
            " ORDER BY name",
    ) { stmt ->
        val names = mutableListOf<String>()
        while (stmt.step()) {
            names.add(stmt.getText(0))
        }
        names
    }
}

actual suspend fun getDbTableRowCount(table: String): Long {
    val safeName = getValidatedTableName(table)
    return AppDatabase.instance.useReaderConnection { c ->
        c.usePrepared("SELECT COUNT(*) FROM `$safeName`") { stmt ->
            if (stmt.step()) stmt.getLong(0) else 0L
        }
    }
}

actual suspend fun getDbTableRows(table: String, offset: Int, limit: Int): List<String> {
    val safeName = getValidatedTableName(table)
    return AppDatabase.instance.useReaderConnection { c ->
        c.usePrepared("SELECT * FROM `$safeName` LIMIT ? OFFSET ?") { stmt ->
            stmt.bindText(1, limit.toString())
            stmt.bindText(2, offset.toString())
            val rows = mutableListOf<String>()
            while (stmt.step()) {
                val obj = JSONObject()
                for (i in 0 until stmt.getColumnCount()) {
                    val col = stmt.getColumnName(i)
                    if (stmt.isNull(i)) obj.put(col, JSONObject.NULL) else obj.put(col, stmt.getText(i))
                }
                rows.add(obj.toString())
            }
            rows
        }
    }
}

actual suspend fun getDbTableInfo(table: String): DbTableInfo =
    DbTableInfo(idKey = primaryKeyColumn(table))

actual suspend fun createDbTableRow(table: String, rowJson: String): Boolean {
    val safeName = getValidatedTableName(table)
    val json = JSONObject(rowJson)
    val keys = json.keys().asSequence().toList()
    require(keys.isNotEmpty()) { "row must not be empty" }
    keys.forEach { requireSafeName(it) }
    val columns = keys.joinToString(", ") { "`$it`" }
    val placeholders = keys.joinToString(", ") { "?" }
    AppDatabase.instance.useWriterConnection { c ->
        c.usePrepared("INSERT INTO `$safeName` ($columns) VALUES ($placeholders)") { stmt ->
            keys.forEachIndexed { i, k -> stmt.bindText(i + 1, json.get(k)?.toString() ?: "") }
            stmt.step()
        }
    }
    return true
}

actual suspend fun deleteDbTableRows(table: String, ids: List<String>): Boolean {
    require(ids.isNotEmpty()) { "ids must not be empty" }
    val safeName = getValidatedTableName(table)
    val idKey = primaryKeyColumn(safeName)
    val placeholders = ids.joinToString(", ") { "?" }
    AppDatabase.instance.useWriterConnection { c ->
        c.usePrepared("DELETE FROM `$safeName` WHERE `$idKey` IN ($placeholders)") { stmt ->
            ids.forEachIndexed { i, id -> stmt.bindText(i + 1, id) }
            stmt.step()
        }
    }
    return true
}
