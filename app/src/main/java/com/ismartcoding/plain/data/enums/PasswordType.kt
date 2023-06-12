package com.ismartcoding.plain.data.enums

import com.ismartcoding.plain.features.locale.LocaleHelper

enum class PasswordType {
    RANDOM,
    FIXED,
    NONE;

    fun getText(): String {
        return LocaleHelper.getString("password_type_" + this.name.lowercase())
    }
}