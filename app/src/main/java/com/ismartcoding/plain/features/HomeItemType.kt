package com.ismartcoding.plain.features

import com.ismartcoding.plain.features.locale.LocaleHelper

enum class HomeItemType(val value: String) {
    EDUCATION("education"),
    NETWORK("network"),
    EXCHANGE("exchange");

    fun getTitle(): String {
        return LocaleHelper.getString("command_title_$value")
    }

    fun getDescription(): String {
        return LocaleHelper.getString("command_description_$value")
    }

    fun canRefresh(): Boolean{
        return setOf(EXCHANGE, NETWORK).contains(this)
    }

    companion object {
        fun parse(input: String): HomeItemType? {
            return values().find { it.value == input }
        }
    }
}