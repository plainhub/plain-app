package com.ismartcoding.plain.httpserver.mainschemas

import com.ismartcoding.plain.features.ClipboardHelper
import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.Context
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.httpserver.http.GraphqlRequestContext
import com.ismartcoding.plain.httpserver.models.ActionResult
import com.ismartcoding.plain.httpserver.models.ClipboardItem
import com.ismartcoding.plain.httpserver.models.toModel
import com.ismartcoding.plain.helpers.QueryHelper
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.isEnabledAsync
import com.ismartcoding.plain.platform.setClipboardText

private suspend fun ensureClipboardEnabled() {
    if (!Permission.CLIPBOARD.isEnabledAsync()) {
        throw GraphQLError("clipboard_sync_disabled")
    }
}

/** Paged clipboard history, newest first. Desktop-side aggregation reads this. */
@GraphQLQuery(description = "Paged clipboard history, newest first.")
suspend fun clipboardItems(offset: Int, limit: Int, query: String): List<ClipboardItem> {
    ensureClipboardEnabled()
    return ClipboardHelper.getPage(limit.coerceIn(1, 200), offset.coerceAtLeast(0), query).map { it.toModel() }
}

@GraphQLQuery
suspend fun clipboardItemCount(query: String): Int {
    ensureClipboardEnabled()
    return ClipboardHelper.count(query)
}

/**
 * Writes [text] into the system clipboard on behalf of the calling client.
 * The write is recorded in the history with the caller as source; the local
 * watcher will see it but skip re-broadcasting (hash dedup), so no loop.
 */
@GraphQLMutation(description = "Write text into the system clipboard and record it in the history with the calling client as source; an identical latest entry is skipped (hash dedup).")
suspend fun setClipboard(text: String, context: Context): Boolean {
    ensureClipboardEnabled()
    val ctx = context.get<GraphqlRequestContext>()
    ClipboardHelper.record(text, source = ctx?.header("c-id") ?: "")
    setClipboardText("plain", text)
    return true
}

/** Deletes clipboard history entries matching the query DSL (`ids:`/`text:`, bare word = text LIKE). Blank query is rejected — send `all:true` to clear the whole history. */
@GraphQLMutation(description = "Delete clipboard history entries matching the query DSL (ids:/text:, bare word = text LIKE); blank query is rejected — send all:true to clear the whole history.")
suspend fun deleteClipboardItems(query: String): ActionResult {
    ensureClipboardEnabled()
    QueryHelper.requireExplicitBulkQuery(query)
    return ActionResult(ClipboardHelper.delete(query))
}

fun SchemaBuilder.addClipboardSchema() {
}
