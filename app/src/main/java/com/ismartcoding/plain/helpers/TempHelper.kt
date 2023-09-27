package com.ismartcoding.plain.helpers

object TempHelper {
    private val dict = mutableMapOf<String, String>()

    fun setValue(
        key: String,
        value: String,
    ) {
        return dict.set(key, value)
    }

    fun getValue(key: String): String {
        return dict.getOrDefault(key, "")
    }

    fun clearValue(key: String) {
        dict.remove(key)
    }
}
