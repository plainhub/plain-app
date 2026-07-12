package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.lib.kgraphql.schema.execution.Executor
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.features.feed.FeedEntryHelper
import com.ismartcoding.plain.features.feed.FeedHelper
import com.ismartcoding.plain.features.feed.exportAsync
import com.ismartcoding.plain.platform.fetchContentAsync
import com.ismartcoding.plain.platform.fetchRssChannel
import com.ismartcoding.plain.features.feed.importAsync
import com.ismartcoding.plain.web.loaders.FeedsLoader
import com.ismartcoding.plain.web.loaders.TagsLoader
import com.ismartcoding.plain.web.models.FeedEntry
import com.ismartcoding.plain.web.models.ID
import com.ismartcoding.plain.web.models.toModel
import com.ismartcoding.plain.platform.feedWorkerOneTimeRequest
import java.io.StringReader
import java.io.StringWriter

fun SchemaBuilder.addFeedSchema() {
    query("feeds") {
        resolver { ->
            val items = FeedHelper.getAll()
            items.map { it.toModel() }
        }
    }
    query("feedsCount") {
        resolver { ->
            FeedHelper.getFeedCounts().map { it.toModel() }
        }
    }
    query("feedEntries") {
        configure {
            executor = Executor.DataLoaderPrepared
        }
        resolver("offset", "limit", "query") { offset: Int, limit: Int, query: String ->
            val items = FeedEntryHelper.search(query, limit, offset)
            items.map { it.toModel() }
        }
        type<FeedEntry> {
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
    query("feedEntryCount") {
        resolver("query") { query: String ->
            FeedEntryHelper.count(query)
        }
    }
    query("feedEntry") {
        resolver("id") { id: ID ->
            val data = FeedEntryHelper.feedEntryDao.getById(id.value)
            data?.toModel()
        }
    }
    mutation("fetchFeedContent") {
        resolver("id") { id: ID ->
            val feed = FeedEntryHelper.feedEntryDao.getById(id.value)
            feed?.fetchContentAsync()
            feed?.toModel()
        }
    }
    mutation("syncFeeds") {
        resolver("id") { id: ID? ->
            feedWorkerOneTimeRequest(id?.value ?: "")
            true
        }
    }
    mutation("updateFeed") {
        resolver("id", "name", "fetchContent") { id: ID, name: String, fetchContent: Boolean ->
            FeedHelper.updateAsync(id.value) {
                this.name = name
                this.fetchContent = fetchContent
            }
            FeedHelper.getById(id.value)?.toModel()
        }
    }
    mutation("createFeed") {
        resolver("url", "fetchContent") { url: String, fetchContent: Boolean ->
            val syndFeed = fetchRssChannel(url)
            val id =
                FeedHelper.addAsync {
                    this.url = url
                    this.name = syndFeed.title ?: ""
                    this.fetchContent = fetchContent
                }
            feedWorkerOneTimeRequest(id)
            FeedHelper.getById(id)
        }
    }
    mutation("importFeeds") {
        resolver("content") { content: String ->
            FeedHelper.importAsync(StringReader(content))
            true
        }
    }
    mutation("exportFeeds") {
        resolver { ->
            val writer = StringWriter()
            FeedHelper.exportAsync(writer)
            writer.toString()
        }
    }
    mutation("deleteFeed") {
        resolver("id") { id: ID ->
            val newIds = setOf(id.value)
            val entryIds = FeedEntryHelper.feedEntryDao.getIds(newIds)
            if (entryIds.isNotEmpty()) {
                TagHelper.deleteTagRelationByKeys(entryIds.toSet(), DataType.FEED_ENTRY)
                FeedEntryHelper.feedEntryDao.deleteByFeedIds(newIds)
            }
            FeedHelper.deleteAsync(newIds)
            true
        }
    }
    mutation("syncFeedContent") {
        resolver("id") { id: ID ->
            val feedEntry = FeedEntryHelper.feedEntryDao.getById(id.value)
            feedEntry?.fetchContentAsync()
            feedEntry?.toModel()
        }
    }
    mutation("deleteFeedEntries") {
        resolver("query") { query: String ->
            val ids = FeedEntryHelper.getIdsAsync(query)
            TagHelper.deleteTagRelationByKeys(ids, DataType.FEED_ENTRY)
            FeedEntryHelper.deleteAsync(ids)
            query
        }
    }
}
