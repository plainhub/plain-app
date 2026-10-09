package com.ismartcoding.plain.api

internal actual object RustCoreBridge {
    init { System.loadLibrary("plain_rust") }
    private external fun startNative(databasePath: String, token: String, configJson: String): String
    private external fun stopNative(): String
    actual fun start(databasePath: String, token: String, configJson: String): String = startNative(databasePath, token, configJson)
    actual fun stop() { val error = stopNative(); check(error.isEmpty()) { error } }
}
