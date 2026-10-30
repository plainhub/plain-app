package com.ismartcoding.plain.features.feeds

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.models.FeedEntry
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.rust.graphql.selectionQuery

object FeedEntriesClient : FeedEntriesApi()

open class FeedEntriesApi internal constructor(protected val api: GraphqlClient = GraphqlApi) {
    suspend fun count(query: String): Int =
        api.execute(Query.feedEntryCount(query))

    suspend fun search(query: String, limit: Int, offset: Int): List<FeedEntry> =
        api.execute(Query.feedEntries(query, limit, offset))

    suspend fun getAsync(id: String): FeedEntry? =
        api.execute(Query.feedEntry(id))

    suspend fun syncContent(id: String): FeedEntry =
        api.execute(Mutation.syncFeedEntryContent(id))

    suspend fun markReadAsync(ids: Set<String>, read: Boolean = true) {
        if (ids.isNotEmpty()) api.execute(Mutation.markFeedEntriesRead(selectionQuery(ids), read))
    }

    suspend fun deleteAsync(ids: Set<String>) {
        if (ids.isNotEmpty()) api.execute(Mutation.deleteFeedEntries(selectionQuery(ids)))
    }

    suspend fun deleteQuery(query: String) {
        api.execute(Mutation.deleteFeedEntries(query))
    }

    suspend fun deleteAllAsync() = deleteQuery("all:true")
}
