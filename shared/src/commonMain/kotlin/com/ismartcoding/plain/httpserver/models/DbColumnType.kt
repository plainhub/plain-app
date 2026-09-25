package com.ismartcoding.plain.httpserver.models

/** Declared column type for `dbTableColumns.dataType`; UNKNOWN covers
 *  undeclared columns and declared types outside SQLite's standard set. */
enum class DbColumnType {
    TEXT,
    INTEGER,
    REAL,
    BLOB,
    NUMERIC,
    UNKNOWN,
}
