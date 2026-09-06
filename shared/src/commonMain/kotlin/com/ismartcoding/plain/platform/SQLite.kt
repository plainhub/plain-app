package com.ismartcoding.plain.platform

import com.ismartcoding.plain.httpserver.models.DbTableInfo

/**
 * Absolute path of the SQLite database file. Empty string on platforms without
 * a filesystem-backed SQLite database.
 */
expect fun getDbPath(): String

/**
 * Returns the list of user tables in the database (excluding system tables like
 * android_metadata, sqlite_sequence, room_master_table).
 */
expect suspend fun getDbTableNames(): List<String>

/**
 * Returns the row count for [table]. Throws IllegalArgumentException if the
 * table does not exist or the name is not a safe identifier.
 */
expect suspend fun getDbTableRowCount(table: String): Long

/**
 * Returns rows of [table] paginated by [offset]/[limit]. Each row is encoded as
 * a JSON object string (column name -> string value, or null).
 * Throws IllegalArgumentException if the table does not exist or the name is
 * not a safe identifier.
 */
expect suspend fun getDbTableRows(table: String, offset: Int, limit: Int): List<String>

/**
 * Returns metadata about [table] including its primary-key column name.
 * Throws IllegalArgumentException if the table does not exist.
 */
expect suspend fun getDbTableInfo(table: String): DbTableInfo

/**
 * Inserts a row into [table] from a JSON object string ([rowJson]).
 * Throws IllegalArgumentException on invalid table name, missing columns, or
 * unsafe column identifiers.
 */
expect suspend fun createDbTableRow(table: String, rowJson: String): Boolean

/**
 * Deletes rows of [table] identified by [ids] (matched against the table's
 * primary-key column). Throws IllegalArgumentException on invalid table or ids.
 */
expect suspend fun deleteDbTableRows(table: String, ids: List<String>): Boolean
