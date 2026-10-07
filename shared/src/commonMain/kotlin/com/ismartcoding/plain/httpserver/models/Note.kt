package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.db.DNote
import kotlin.time.Instant

data class Note(
    val id: ID,
    val title: String,
    val content: String,
    val deletedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun DNote.toModel(): Note {
    return Note(ID(id), title, content, deletedAt, createdAt, updatedAt)
}