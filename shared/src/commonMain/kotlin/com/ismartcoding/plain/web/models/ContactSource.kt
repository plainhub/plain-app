package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.data.DContactSource
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType

@GraphQLType
data class ContactSource(var name: String, var type: String)

fun DContactSource.toModel(): ContactSource {
    return ContactSource(name, type)
}
