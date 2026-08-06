package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import com.ismartcoding.plain.platform.DSmsCounts

@GraphQLType
data class SmsCounts(
    val total: Int,
    val inbox: Int,
    val sent: Int,
    val drafts: Int,
)

fun DSmsCounts.toModel(): SmsCounts {
    return SmsCounts(total, inbox, sent, drafts)
}