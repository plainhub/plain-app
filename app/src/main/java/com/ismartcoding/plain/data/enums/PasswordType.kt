package com.ismartcoding.plain.data.enums

import com.ismartcoding.plain.features.locale.LocaleHelper

enum class PasswordType(val value: Int) {
    RANDOM(0),
    FIXED(1),
    NONE(2),
    ;

    fun getText(): String {
        return LocaleHelper.getString("password_type_" + this.name.lowercase())
    }

    companion object {
        fun getText(value: Int): String {
            return parse(value).getText()
        }

        fun parse(value: Int): PasswordType {
            return PasswordType.values().find { it.value == value } ?: RANDOM
        }
    }
}
