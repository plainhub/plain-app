package com.ismartcoding.plain.api

internal expect object RustCoreBridge {
    fun start(databasePath: String, token: String): Int
}
