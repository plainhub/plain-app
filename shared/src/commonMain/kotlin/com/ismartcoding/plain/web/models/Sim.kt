package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.data.DSim
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType

@GraphQLType
data class Sim(val id: ID, val label: String, val number: String, val subscriptionId: Int)

fun DSim.toModel(): Sim {
    return Sim(ID(id), label, number, subscriptionId)
}
