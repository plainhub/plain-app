package com.ismartcoding.lib.kgraphql.schema.dsl.operations

import com.ismartcoding.lib.kgraphql.schema.model.QueryDef

class QueryDSL(
    name: String
) : AbstractOperationDSL(name) {


    internal fun toKQLQuery(): QueryDef<out Any?> {
        val function =
            functionWrapper ?: throw IllegalArgumentException("resolver has to be specified for query [$name]")

        return QueryDef(
            name = name,
            resolver = function,
            description = description,
            isDeprecated = isDeprecated,
            deprecationReason = deprecationReason,
            inputValues = inputValues,
            accessRule = accessRuleBlock,
            explicitReturnType = explicitReturnType
        )
    }
}
