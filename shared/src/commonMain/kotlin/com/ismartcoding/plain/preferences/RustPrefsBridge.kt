package com.ismartcoding.plain.preferences

internal expect object RustPrefsBridge {
    fun open(systemPath: String, userPath: String)
    fun systemSnapshot(): String
    fun userSnapshot(): String
    fun setSystem(key: String, valueJson: String)
    fun setUser(key: String, valueJson: String)
    fun removeSystem(key: String)
    fun removeUser(key: String)
}
