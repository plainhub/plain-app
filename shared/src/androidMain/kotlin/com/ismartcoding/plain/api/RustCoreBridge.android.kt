package com.ismartcoding.plain.api

internal actual object RustCoreBridge {
    init { System.loadLibrary("plain_rust") }
    private external fun tlsNative(configJson: String): String
    actual fun tls(configJson: String): String {
        val value = tlsNative(configJson)
        check(!value.startsWith("ERROR:")) { value.removePrefix("ERROR:") }
        return value
    }
    private external fun startPublicNative(configJson: String): String
    private external fun stopPublicNative(): String
    actual fun startPublic(configJson: String): String {
        val result = startPublicNative(configJson)
        check(!result.startsWith("ERROR:")) { result.removePrefix("ERROR:") }
        return result
    }
    actual fun stopPublic() { val error = stopPublicNative(); check(error.isEmpty()) { error } }
    private external fun startNative(databasePath: String, token: String): String
    actual fun start(databasePath: String, token: String): Int {
        val result = startNative(databasePath, token)
        check(!result.startsWith("ERROR:")) { result.removePrefix("ERROR:") }
        return result.toInt()
    }
}
