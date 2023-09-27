package com.ismartcoding.plain.features.rule

import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.locale.LocaleHelper.getString

enum class RuleAction(val value: String) {
    ALLOW("allow"),
    BLOCK("block"),
    ;

    fun getText(): String {
        if (this == ALLOW) {
            return getString(R.string.allow)
        } else if (this == BLOCK) {
            return getString(R.string.block)
        }
        return ""
    }

    companion object {
        fun parse(
            value: String,
            default: RuleAction = ALLOW,
        ): RuleAction {
            return values().find { it.value == value } ?: default
        }
    }
}
