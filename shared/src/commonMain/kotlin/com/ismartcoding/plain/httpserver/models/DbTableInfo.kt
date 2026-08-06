package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType

@GraphQLType
data class DbTableInfo(val idKey: String)
