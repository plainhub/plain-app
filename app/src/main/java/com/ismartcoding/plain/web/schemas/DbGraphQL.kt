package com.ismartcoding.plain.web.schemas

import com.ismartcoding.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.db.AppDatabase
import org.json.JSONObject

private val ALLOWED_NAME_REGEX = Regex("^[a-zA-Z_][a-zA-Z0-9_]*$")

private fun requireSafeName(name: String) {
    require(ALLOWED_NAME_REGEX.matches(name)) { "Invalid identifier: $name" }
}

private fun getValidatedTableName(table: String): String {
    requireSafeName(table)
    val db = AppDatabase.instance.openHelper.readableDatabase
    val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(table))
    val exists = cursor.use { it.moveToFirst() }
    require(exists) { "Table not found: $table" }
    return table
}

private fun dbTableNames(): List<String> {
    val db = AppDatabase.instance.openHelper.readableDatabase
    val cursor = db.query(
        "SELECT name FROM sqlite_master WHERE type='table'" +
            " AND name NOT LIKE 'android_%' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'room_%'" +
            " ORDER BY name",
        emptyArray(),
    )
    val names = mutableListOf<String>()
    cursor.use { c ->
        while (c.moveToNext()) {
            names.add(c.getString(0))
        }
    }
    return names
}

private fun dbTableRowCount(table: String): Long {
    val safeName = getValidatedTableName(table)
    val db = AppDatabase.instance.openHelper.readableDatabase
    val cursor = db.query("SELECT COUNT(*) FROM `$safeName`", emptyArray())
    return cursor.use { c -> if (c.moveToFirst()) c.getLong(0) else 0L }
}

private fun dbTableRows(table: String, offset: Int, limit: Int): List<String> {
    val safeName = getValidatedTableName(table)
    val db = AppDatabase.instance.openHelper.readableDatabase
    val cursor = db.query(
        "SELECT * FROM `$safeName` LIMIT ? OFFSET ?",
        arrayOf(limit.toString(), offset.toString()),
    )
    val rows = mutableListOf<String>()
    cursor.use { c ->
        while (c.moveToNext()) {
            val obj = JSONObject()
            for (i in 0 until c.columnCount) {
                val col = c.getColumnName(i)
                if (c.isNull(i)) obj.put(col, JSONObject.NULL) else obj.put(col, c.getString(i))
            }
            rows.add(obj.toString())
        }
    }
    return rows
}

fun SchemaBuilder.addDbSchema() {
    query("dbPath") {
        resolver { ->
            AppDatabase.instance.openHelper.readableDatabase.path ?: ""
        }
    }

    query("dbTables") {
        resolver { -> dbTableNames() }
    }

    query("dbTableRowCount") {
        resolver { table: String -> dbTableRowCount(table) }
    }

    query("dbTableRows") {
        resolver { table: String, offset: Int, limit: Int -> dbTableRows(table, offset, limit) }
    }

    mutation("createDbTableRow") {
        resolver { table: String, row: String ->
            val safeName = getValidatedTableName(table)
            val json = JSONObject(row)
            val keys = json.keys().asSequence().toList()
            require(keys.isNotEmpty()) { "row must not be empty" }
            keys.forEach { requireSafeName(it) }
            val columns = keys.joinToString(", ") { "`$it`" }
            val placeholders = keys.joinToString(", ") { "?" }
            val args = keys.map { json.get(it)?.toString() ?: "" }.toTypedArray()
            val db = AppDatabase.instance.openHelper.writableDatabase
            db.execSQL("INSERT INTO `$safeName` ($columns) VALUES ($placeholders)", args)
            true
        }
    }

    mutation("deleteDbTableRows") {
        resolver { table: String, ids: List<String> ->
            require(ids.isNotEmpty()) { "ids must not be empty" }
            val safeName = getValidatedTableName(table)
            val placeholders = ids.joinToString(", ") { "?" }
            val db = AppDatabase.instance.openHelper.writableDatabase
            db.execSQL("DELETE FROM `$safeName` WHERE id IN ($placeholders)", ids.toTypedArray())
            true
        }
    }
}
