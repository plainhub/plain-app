package com.ismartcoding.plain.features.rule

import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.plain.data.enums.ConfigType
import com.ismartcoding.plain.features.ApplyTo
import com.ismartcoding.plain.features.Target
import com.ismartcoding.plain.type.ConfigInput

data class RuleEdit(
    var action: RuleAction,
    var applyTo: ApplyTo,
    var localPort: String,
    var direction: RuleDirection,
    var protocol: RuleProtocol,
    var target: Target,
    var isEnabled: Boolean,
    var notes: String,
) {
    fun toRuleInput(): ConfigInput {
        return ConfigInput(
            ConfigType.RULE.value,
            JsonHelper.jsonEncode(
                RuleInput(
                    action.value,
                    applyTo.toValue(),
                    "",
                    direction.value,
                    protocol.value,
                    target.toValue(),
                    isEnabled,
                    notes,
                ),
            ),
        )
    }

    companion object {
        fun createDefault(): RuleEdit {
            return RuleEdit(
                RuleAction.BLOCK,
                ApplyTo(),
                "",
                RuleDirection.INBOUND,
                RuleProtocol.ALL,
                Target(),
                true,
                "",
            )
        }
    }
}
