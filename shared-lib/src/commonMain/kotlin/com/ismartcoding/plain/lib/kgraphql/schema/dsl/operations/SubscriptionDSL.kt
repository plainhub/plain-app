package com.ismartcoding.plain.lib.kgraphql.schema.dsl.operations

import com.ismartcoding.plain.lib.kgraphql.schema.model.FunctionWrapper
import com.ismartcoding.plain.lib.kgraphql.schema.model.SubscriptionDef


class SubscriptionDSL(
    name: String
) : AbstractOperationDSL(name) {

    internal fun toKQLSubscription(): SubscriptionDef<out Any?> {
        val function =
            functionWrapper ?: throw IllegalArgumentException("resolver has to be specified for query [$name]")

        return SubscriptionDef(
            name = name,
            resolver = function,
            description = description,
            isDeprecated = isDeprecated,
            deprecationReason = deprecationReason,
            inputValues = inputValues,
            accessRule = accessRuleBlock
        )
    }
}