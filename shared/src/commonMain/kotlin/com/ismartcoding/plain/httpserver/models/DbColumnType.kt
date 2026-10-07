package com.ismartcoding.plain.httpserver.models

import kotlinx.serialization.Serializable
/** Declared column type for `dbTableColumns.dataType`; UNKNOWN covers
 *  undeclared columns and declared types outside SQLite's standard set. */
@Serializable
enum class DbColumnType {
    TEXT,
    INTEGER,
    REAL,
    BLOB,
    NUMERIC,
    UNKNOWN,
}
