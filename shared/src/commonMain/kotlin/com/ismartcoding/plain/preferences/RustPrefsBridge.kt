package com.ismartcoding.plain.preferences

internal expect object RustPrefsBridge {
    fun open(systemPath: String, userPath: String)
}
