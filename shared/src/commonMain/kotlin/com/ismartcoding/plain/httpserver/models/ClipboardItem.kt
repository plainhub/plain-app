package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.db.DClipboard
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLField
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@GraphQLType
@Serializable
data class ClipboardItem(
    val id: ID,
    val text: String,
    @GraphQLField(description = "Client id of the sync origin: empty = captured locally on this device, otherwise received from that peer.")
    val source: String,
    val label: String,
    val sensitive: Boolean,
    val createdAt: Instant,
)

fun DClipboard.toModel(): ClipboardItem {
    return ClipboardItem(ID(id), text, source, label, sensitive, createdAt)
}
