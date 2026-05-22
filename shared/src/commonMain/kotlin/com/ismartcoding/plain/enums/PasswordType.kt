package com.ismartcoding.plain.enums

enum class PasswordType(val value: Int) {
    NONE(2),
    FIXED(1),
    ;

    companion object {
        fun parse(value: Int): PasswordType {
            return entries.find { it.value == value } ?: FIXED
        }
    }
}
