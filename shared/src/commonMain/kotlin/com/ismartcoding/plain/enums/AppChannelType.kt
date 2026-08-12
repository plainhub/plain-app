package com.ismartcoding.plain.enums

enum class AppChannelType {
    GITHUB,
    GOOGLE,
    FDROID;
    companion object {
        fun fromString(value: String): AppChannelType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: GITHUB
        }
    }
}