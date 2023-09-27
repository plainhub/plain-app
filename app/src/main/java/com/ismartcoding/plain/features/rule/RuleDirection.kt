package com.ismartcoding.plain.features.rule

import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.locale.LocaleHelper

enum class RuleDirection(val value: String) {
    INBOUND("inbound"),
    OUTBOUND("outbound"),
    ;

    fun getText(): String {
        if (this == INBOUND) {
            return LocaleHelper.getString(R.string.inbound)
        } else if (this == OUTBOUND) {
            return LocaleHelper.getString(R.string.outbound)
        }
        return ""
    }

    companion object {
        fun parse(
            value: String,
            default: RuleDirection = INBOUND,
        ): RuleDirection {
            return values().find { it.value == value } ?: default
        }
    }
}
