package com.ismartcoding.plain.features.feeds

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.models.Feed
import com.ismartcoding.plain.models.FeedPreview
import com.ismartcoding.plain.models.FeedSyncState
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import com.ismartcoding.plain.rust.graphql.GraphqlApi

internal object FeedsClient : FeedsApi()
internal open class FeedsApi(
    private val api: GraphqlClient = GraphqlApi,
) {

    suspend fun getAll(withCount: Boolean = false): List<Feed> {
        val data = api.execute(Query.feeds(withCount))
        val counts = data.feedEntryCounts.associate { it.id to it.count }
        return data.feeds.map { it.apply { entryCount = counts[id] ?: 0 } }
    }

    suspend fun getById(id: String): Feed? =
        api.execute(Query.feed(id))

    suspend fun preview(url: String): FeedPreview =
        api.execute(Query.feedPreview(url))

    suspend fun create(url: String, name: String, fetchContent: Boolean = false): Feed =
        api.execute(Mutation.createFeed(url, name, fetchContent))

    suspend fun update(id: String, name: String? = null, url: String? = null, fetchContent: Boolean? = null): Feed {
        return api.execute(Mutation.updateFeed(id, name, url, fetchContent))
    }

    suspend fun deleteAsync(ids: Set<String>) {
        ids.forEach { api.execute(Mutation.deleteFeed(it)) }
    }

    suspend fun importAsync(content: String) {
        api.execute(Mutation.importFeeds(content))
    }

    suspend fun exportAsync(): String = api.execute(Mutation.exportFeeds())

    suspend fun sync(feedId: String) {
        api.execute(Mutation.syncFeeds(feedId))
    }

    suspend fun syncStates(): List<FeedSyncState> =
        api.execute(Query.feedSyncStates())
}
