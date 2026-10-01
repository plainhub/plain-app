package com.ismartcoding.plain.preferences

internal actual object RustPrefsBridge {
    init {
        System.loadLibrary("plain_rust")
    }

    private external fun openNative(path: String)
    private external fun snapshotNative(): String
    private external fun setNative(key: String, valueJson: String)
    private external fun removeNative(key: String)

    actual fun open(path: String) = openNative(path)
    actual fun snapshot(): String = snapshotNative()
    actual fun set(key: String, valueJson: String) = setNative(key, valueJson)
    actual fun remove(key: String) = removeNative(key)
}
