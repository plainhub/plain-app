package com.ismartcoding.plain.httpserver.mainschemas

import kotlinx.serialization.json.*

import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.features.feed.FeedEntryHelper
import com.ismartcoding.plain.features.feed.FeedHelper
import com.ismartcoding.plain.features.feed.exportAsync
import com.ismartcoding.plain.platform.fetchContentAsync
import com.ismartcoding.plain.platform.fetchRssChannel
import com.ismartcoding.plain.features.feed.importAsync
import com.ismartcoding.plain.helpers.QueryHelper
import com.ismartcoding.plain.httpserver.loaders.FeedsLoader
import com.ismartcoding.plain.httpserver.loaders.TagsLoader
import com.ismartcoding.plain.httpserver.models.Feed
import com.ismartcoding.plain.httpserver.models.ActionResult
import com.ismartcoding.plain.httpserver.models.FeedEntryCount
import com.ismartcoding.plain.httpserver.models.FeedEntry
import com.ismartcoding.plain.httpserver.models.ID
import com.ismartcoding.plain.httpserver.models.toModel
import com.ismartcoding.plain.platform.feedWorkerOneTimeRequest
import kotlin.reflect.typeOf

@GraphQLQuery
suspend fun feeds(): List<Feed> {
    val items = FeedHelper.getAll()
    return items.map { it.toModel() }
}

@GraphQLQuery
suspend fun feedEntryCounts(): List<FeedEntryCount> {
    return FeedHelper.getFeedCounts().map { it.toModel() }
}

@GraphQLQuery
suspend fun feedEntryCount(query: String): Int {
    return FeedEntryHelper.count(query)
}

@GraphQLQuery
suspend fun feedEntry(id: ID): FeedEntry? {
    val data = FeedEntryHelper.getAsync(id.value)
    return data?.toModel()
}

@GraphQLMutation(description = "Queue a feed content sync. `id` is optional: omit it (null) to sync all feeds, or pass one feed id to sync only that feed.")
suspend fun syncFeeds(id: ID?): Boolean {
    feedWorkerOneTimeRequest(id?.value ?: "")
    return true
}

@GraphQLMutation
suspend fun updateFeed(id: ID, name: String, fetchContent: Boolean): Feed {
    FeedHelper.updateAsync(id.value) {
        this.name = name
        this.fetchContent = fetchContent
    }
    return FeedHelper.getById(id.value)?.toModel()
        ?: throw GraphQLError("Feed ${id.value} not found after update")
}

@GraphQLMutation
suspend fun createFeed(url: String, fetchContent: Boolean): Feed {
    val id = FeedHelper.addAsync { this.url = url; this.fetchContent = fetchContent }

    return FeedHelper.getById(id)?.toModel() ?: throw GraphQLError("Feed $id not found after create")
}

@GraphQLMutation
suspend fun importFeeds(content: String): Boolean {
    FeedHelper.importAsync(content)
    return true
}

@GraphQLMutation
suspend fun exportFeeds(): String {
    return FeedHelper.exportAsync()
}

@GraphQLMutation
suspend fun deleteFeed(id: ID): Boolean {
    return com.ismartcoding.plain.api.RustContentApi.mutate("deleteFeed(id: ${com.ismartcoding.plain.api.gql(id.value)})").getValue("deleteFeed").jsonPrimitive.boolean
}

@GraphQLMutation
suspend fun syncFeedEntryContent(id: ID): FeedEntry {
    return FeedEntryHelper.syncContent(id.value).toModel()
}

@GraphQLMutation
suspend fun deleteFeedEntries(query: String): ActionResult {
    QueryHelper.requireExplicitBulkQuery(query)
    val result = com.ismartcoding.plain.api.RustContentApi.mutate("deleteFeedEntries(query: ${com.ismartcoding.plain.api.gql(query)}) { affectedCount }")
    return ActionResult(result.getValue("deleteFeedEntries").jsonObject.getValue("affectedCount").jsonPrimitive.int)
}

@GraphQLQuery
suspend fun feedEntries(offset: Int, limit: Int, query: String): List<FeedEntry> {
    val items = FeedEntryHelper.search(query, limit, offset)
    return items.map { it.toModel() }
}

fun SchemaBuilder.addFeedSchema() {
    type<FeedEntryCount> {
        property("id", typeOf<ID>(), { it: FeedEntryCount -> ID(it.id) })
    }
    type<FeedEntry> {
        property("feedId", typeOf<ID>(), { it: FeedEntry -> ID(it.feedId) })
        dataProperty("tags") {
            prepare { item -> item.id.value }
            loader { ids ->
                TagsLoader.load(ids, DataType.FEED_ENTRY)
            }
        }
        dataProperty("feed") {
            prepare { item -> item.feedId }
            loader { ids ->
                FeedsLoader.load(ids)
            }
        }
    }
}

@GraphQLQuery
suspend fun feed(id: ID): Feed? = FeedHelper.getById(id.value)?.toModel()

@GraphQLQuery
suspend fun feedSyncStates(): List<com.ismartcoding.plain.httpserver.models.FeedSyncState> = com.ismartcoding.plain.api.RustContentApi.query("feedSyncStates { feedId status error }").getValue("feedSyncStates").let { values ->
    values.jsonArray.map { val row = it.jsonObject; com.ismartcoding.plain.httpserver.models.FeedSyncState(ID(row.getValue("feedId").jsonPrimitive.content), row.getValue("status").jsonPrimitive.content, row.getValue("error").jsonPrimitive.content) }
}

@GraphQLMutation
suspend fun updateFeedUrl(id: ID, url: String): Feed {
    FeedHelper.updateAsync(id.value) { this.url = url }
    return FeedHelper.getById(id.value)?.toModel() ?: throw GraphQLError("Feed not found")
}

@GraphQLMutation
suspend fun markFeedEntriesRead(query: String, read: Boolean): ActionResult {
    QueryHelper.requireExplicitBulkQuery(query)
    val ids = FeedEntryHelper.getIdsAsync(query)
    FeedEntryHelper.markReadAsync(ids, read)
    return ActionResult(ids.size)
}
