package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.db.DClipboard
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@GraphQLType
@Serializable
data class Clipboard(
    val id: ID,
    val text: String,
    val source: String,
    val label: String,
    val sensitive: Boolean,
    val createdAt: Instant,
)

fun DClipboard.toModel(): Clipboard {
    return Clipboard(ID(id), text, source, label, sensitive, createdAt)
}
