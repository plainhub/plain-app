package com.ismartcoding.plain.ui.page.playlist.components

import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.features.file.FileSortBy

/**
 * In-memory [DAudio] ordering for lists that are not DB-driven (playlist
 * rows), matching the FileSortBy options the sort dialog offers.
 */
fun List<DAudio>.sortedByAudio(sortBy: FileSortBy): List<DAudio> {
    val ascending = sortBy == FileSortBy.DATE_ASC || sortBy == FileSortBy.SIZE_ASC || sortBy == FileSortBy.NAME_ASC
    val base = when (sortBy) {
        FileSortBy.DATE_ASC, FileSortBy.DATE_DESC -> sortedBy { it.createdAt }
        FileSortBy.SIZE_ASC, FileSortBy.SIZE_DESC -> sortedBy { it.size }
        else -> sortedBy { it.title.lowercase() }
    }
    return if (ascending) base else base.asReversed()
}
