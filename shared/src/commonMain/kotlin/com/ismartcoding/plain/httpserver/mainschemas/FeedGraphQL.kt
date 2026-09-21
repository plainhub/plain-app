package com.ismartcoding.plain.httpserver.mainschemas

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
    val data = FeedEntryHelper.feedEntryDao.getById(id.value)
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
    val syndFeed = fetchRssChannel(url)
    val id =
        FeedHelper.addAsync {
            this.url = url
            this.name = syndFeed.title ?: ""
            this.fetchContent = fetchContent
        }
    feedWorkerOneTimeRequest(id)
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
    val newIds = setOf(id.value)
    val entryIds = FeedEntryHelper.feedEntryDao.getIds(newIds)
    if (entryIds.isNotEmpty()) {
        TagHelper.deleteTagRelationByKeys(entryIds.toSet(), DataType.FEED_ENTRY)
        FeedEntryHelper.deleteByFeedIdsAsync(newIds)
    }
    FeedHelper.deleteAsync(newIds)
    return true
}

@GraphQLMutation
suspend fun syncFeedContent(id: ID): FeedEntry {
    val feedEntry = FeedEntryHelper.feedEntryDao.getById(id.value)
        ?: throw GraphQLError("Feed entry ${id.value} not found")
    feedEntry.fetchContentAsync()
    return feedEntry.toModel()
}

@GraphQLMutation
suspend fun deleteFeedEntries(query: String): ActionResult {
    QueryHelper.requireExplicitBulkQuery(query)
    val ids = FeedEntryHelper.getIdsAsync(query)
    TagHelper.deleteTagRelationByKeys(ids, DataType.FEED_ENTRY)
    FeedEntryHelper.deleteAsync(ids)
    return ActionResult(ids.size)
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
