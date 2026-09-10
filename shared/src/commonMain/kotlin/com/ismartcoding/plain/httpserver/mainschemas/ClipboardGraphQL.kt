package com.ismartcoding.plain.httpserver.mainschemas

import com.ismartcoding.plain.db.DClipboard
import com.ismartcoding.plain.features.ClipboardHelper
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.crypto.sha256
import com.ismartcoding.plain.lib.generateId
import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.Context
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.httpserver.http.GraphqlRequestContext
import com.ismartcoding.plain.httpserver.models.Clipboard
import com.ismartcoding.plain.httpserver.models.ID
import com.ismartcoding.plain.httpserver.models.toModel
import com.ismartcoding.plain.platform.setClipboardText
import com.ismartcoding.plain.preferences.ClipboardSyncPreference

private const val MAX_TEXT_LENGTH = 256 * 1024

private suspend fun ensureClipboardEnabled() {
    if (!ClipboardSyncPreference.getAsync()) {
        throw GraphQLError("clipboard_sync_disabled")
    }
}

private fun hashOf(text: String): String =
    sha256(text.encodeToByteArray()).joinToString("") { it.toString(16).padStart(2, '0') }

/** Paged clipboard history, newest first. Desktop-side aggregation reads this. */
@GraphQLQuery
suspend fun clipboard(offset: Int, limit: Int, query: String): List<Clipboard> {
    ensureClipboardEnabled()
    return ClipboardHelper.getPage(limit.coerceIn(1, 200), offset.coerceAtLeast(0), query).map { it.toModel() }
}

@GraphQLQuery
suspend fun clipboardCount(query: String): Int {
    ensureClipboardEnabled()
    return ClipboardHelper.count(query)
}

/**
 * Writes [text] into the system clipboard on behalf of the calling client.
 * The write is recorded in the history with the caller as source; the local
 * watcher will see it but skip re-broadcasting (hash dedup), so no loop.
 */
@GraphQLMutation
suspend fun setClipboard(text: String, context: Context): Boolean {
    ensureClipboardEnabled()
    if (text.isBlank() || text.length > MAX_TEXT_LENGTH) {
        throw GraphQLError("invalid_clipboard_text")
    }
    val hash = hashOf(text)
    if (ClipboardHelper.getLatestByHash(hash) == null) {
        val ctx = context.get<GraphqlRequestContext>()
        val entry = DClipboard(
            id = generateId(),
            text = text,
            hash = hash,
            source = ctx?.header("c-id") ?: "",
            createdAt = TimeHelper.now(),
        )
        ClipboardHelper.insert(entry)
    }
    setClipboardText("plain", text)
    return true
}

/** Deletes clipboard history entries by ids. */
@GraphQLMutation
suspend fun cancelClipboard(ids: List<ID>): Boolean {
    ensureClipboardEnabled()
    ClipboardHelper.deleteByIds(ids.map { it.value })
    return true
}

fun SchemaBuilder.addClipboardSchema() {
}
