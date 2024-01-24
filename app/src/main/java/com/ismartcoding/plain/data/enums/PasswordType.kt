package com.ismartcoding.plain.data.enums

import com.ismartcoding.plain.features.locale.LocaleHelper

enum class PasswordType(val value: Int) {
    NONE(2),
    FIXED(1),
    ;

    fun getText(): String {
        return LocaleHelper.getString("password_type_" + this.name.lowercase())
    }

    companion object {
        fun getText(value: Int): String {
            return parse(value).getText()
        }

        fun parse(value: Int): PasswordType {
            return entries.find { it.value == value } ?: FIXED
        }
    }
}
