package com.ismartcoding.plain.helpers

object TempHelper {
    private val dict = mutableMapOf<String, String>()

    fun setValue(
        key: String,
        value: String,
    ) {
        dict[key] = value
    }

    fun getValue(key: String): String {
        return dict[key] ?: ""
    }

    fun clearValue(key: String) {
        dict.remove(key)
    }
}