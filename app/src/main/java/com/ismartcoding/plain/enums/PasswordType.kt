package com.ismartcoding.plain.enums

import com.ismartcoding.plain.features.locale.LocaleHelper

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
