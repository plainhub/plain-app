package com.ismartcoding.plain.lib.kgraphql.schema.model

import com.ismartcoding.plain.lib.kgraphql.Context
import kotlin.reflect.KType

class MutationDef<R> (
    name : String,
    resolver: FunctionWrapper<R>,
    override val description : String?,
    override val isDeprecated: Boolean,
    override val deprecationReason: String?,
    accessRule: ((Nothing?, Context) -> Exception?)? = null,
    inputValues : List<InputValueDef<*>> = emptyList(),
    explicitReturnType: KType? = null
) : BaseOperationDef<Nothing, R>(name, resolver, inputValues, accessRule, explicitReturnType), DescribedDef
