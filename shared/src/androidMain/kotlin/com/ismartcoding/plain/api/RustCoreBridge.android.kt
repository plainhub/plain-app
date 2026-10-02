package com.ismartcoding.plain.api

internal actual object RustCoreBridge {
    init { System.loadLibrary("plain_rust") }
    private external fun startNative(databasePath: String, token: String): String
    actual fun start(databasePath: String, token: String): Int {
        val result = startNative(databasePath, token)
        check(!result.startsWith("ERROR:")) { result.removePrefix("ERROR:") }
        return result.toInt()
    }
}
