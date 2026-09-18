package com.ismartcoding.plain.ui.page.playlist

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.db.DAudioPlaylistItem
import com.ismartcoding.plain.i18n.*
import org.jetbrains.compose.resources.stringResource

/** In-memory sort orders for the playlist detail track list; custom = manual order. */
enum class PlaylistSortOrder {
    CUSTOM,
    TITLE_ASC,
    TITLE_DESC;

    /** Sort cycles custom -> A-Z -> Z-A -> custom. */
    fun next(): PlaylistSortOrder = entries[(ordinal + 1) % entries.size]

    @Composable
    fun label(): String = when (this) {
        CUSTOM -> stringResource(Res.string.sort_custom)
        TITLE_ASC -> stringResource(Res.string.sort_title_asc)
        TITLE_DESC -> stringResource(Res.string.sort_title_desc)
    }
}

fun List<DAudioPlaylistItem>.sortedByOrder(order: PlaylistSortOrder): List<DAudioPlaylistItem> = when (order) {
    PlaylistSortOrder.CUSTOM -> this
    PlaylistSortOrder.TITLE_ASC -> sortedBy { it.title.lowercase() }
    PlaylistSortOrder.TITLE_DESC -> sortedByDescending { it.title.lowercase() }
}
