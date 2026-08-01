package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType

@GraphQLType
data class ActionResult(val type: DataType, val query: String)