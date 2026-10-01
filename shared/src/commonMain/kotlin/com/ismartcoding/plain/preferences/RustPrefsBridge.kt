package com.ismartcoding.plain.preferences

internal expect object RustPrefsBridge {
    fun open(path: String)
    fun snapshot(): String
    fun set(key: String, valueJson: String)
    fun remove(key: String)
}
