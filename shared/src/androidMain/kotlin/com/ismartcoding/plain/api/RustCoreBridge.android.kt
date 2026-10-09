package com.ismartcoding.plain.api

internal actual object RustCoreBridge {
    init { System.loadLibrary("plain_rust") }
    private external fun startPublicNative(configJson: String): String
    private external fun stopPublicNative(): String
    actual fun startPublic(configJson: String): String = startPublicNative(configJson)
    actual fun stopPublic() { val error = stopPublicNative(); check(error.isEmpty()) { error } }
    private external fun startNative(databasePath: String, token: String): String
    actual fun start(databasePath: String, token: String): Int {
        val result = startNative(databasePath, token)
        check(!result.startsWith("ERROR:")) { result.removePrefix("ERROR:") }
        return result.toInt()
    }
}
