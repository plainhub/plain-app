package com.ismartcoding.plain.api

internal expect object RustCoreBridge {
    fun startPublic(configJson: String): String
    fun stopPublic()
    fun start(databasePath: String, token: String): Int
}
