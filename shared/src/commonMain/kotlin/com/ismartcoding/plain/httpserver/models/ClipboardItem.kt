package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.db.DClipboard
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class ClipboardItem(
    val id: ID,
    val text: String,
    val source: String,
    val label: String,
    val sensitive: Boolean,
    val createdAt: Instant,
)

fun DClipboard.toModel(): ClipboardItem {
    return ClipboardItem(ID(id), text, source, label, sensitive, createdAt)
}
