package com.ismartcoding.plain.features.clipboard

import com.ismartcoding.plain.features.chat.peer.RustPeerStore
import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.models.ClipboardItem
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.rust.graphql.selectionQuery

object ClipboardClient {
    suspend fun getPage(limit: Int, offset: Int, query: String = ""): List<ClipboardItem> =
        GraphqlApi.execute(Query.clipboardItems(limit, offset, query))

    suspend fun count(query: String = ""): Int =
        GraphqlApi.execute(Query.clipboardItemCount(query))

    suspend fun deleteByIds(ids: List<String>): Int {
        if (ids.isEmpty()) return 0
        return delete(selectionQuery(ids.toSet()))
    }

    suspend fun delete(query: String): Int =
        GraphqlApi.execute(Mutation.deleteClipboardItems(query)).affectedCount

    suspend fun clear() {
        delete("all:true")
    }

    suspend fun getSourceName(source: String): String =
        if (source.isBlank()) "" else RustPeerStore.getById(source)?.name ?: source
}
