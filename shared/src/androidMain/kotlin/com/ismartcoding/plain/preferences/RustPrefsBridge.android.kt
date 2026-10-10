package com.ismartcoding.plain.preferences

internal actual object RustPrefsBridge {
    init { System.loadLibrary("plain_rust") }
    private external fun openNative(systemPath: String, userPath: String)
    actual fun open(systemPath: String, userPath: String) = openNative(systemPath, userPath)
}
