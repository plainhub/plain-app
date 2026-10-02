package com.ismartcoding.plain.features.feed

import com.ismartcoding.plain.api.*
import com.ismartcoding.plain.db.*
import com.ismartcoding.plain.platform.feedWorkerOneTimeRequest
import kotlinx.serialization.json.*

object FeedHelper {
    suspend fun getAll(): List<DFeed> = RustContentApi.query("feeds { $FEED_FIELDS }").getValue("feeds").jsonArray.map { it.feed() }
    suspend fun getFeedCounts(): List<DFeedCount> = RustContentApi.query("feedEntryCounts { id count }").getValue("feedEntryCounts").jsonArray.map { DFeedCount(it.jsonObject.string("id"), it.jsonObject.getValue("count").jsonPrimitive.int) }
    suspend fun getById(id: String): DFeed? = RustContentApi.query("feed(id: ${gql(id)}) { $FEED_FIELDS }")["feed"]?.takeUnless { it is JsonNull }?.feed()
    suspend fun getByUrl(url: String): DFeed? = getAll().firstOrNull { it.url == url }
    suspend fun addAsync(updateItem: DFeed.() -> Unit): String {
        val item = DFeed().apply(updateItem)
        val created = RustContentApi.mutate("createFeed(url: ${gql(item.url)}, fetchContent: ${item.fetchContent}) { $FEED_FIELDS }").getValue("createFeed").feed()
        if (created.name != item.name && item.name.isNotEmpty()) updateAsync(created.id) { name = item.name }
        return created.id
    }
    suspend fun updateAsync(id: String, updateItem: DFeed.() -> Unit): String {
        val current = getById(id) ?: error("Feed $id not found")
        val oldUrl = current.url
        current.updateItem()
        if (oldUrl != current.url) RustContentApi.mutate("updateFeedUrl(id: ${gql(id)}, url: ${gql(current.url)}) { id }")
        RustContentApi.mutate("updateFeed(id: ${gql(id)}, name: ${gql(current.name)}, fetchContent: ${current.fetchContent}) { id }")
        return id
    }
    suspend fun deleteAsync(ids: Set<String>) { ids.forEach { RustContentApi.mutate("deleteFeed(id: ${gql(it)})"); FeedWorkerState.clear(it) } }
    fun fetchOneTime(feedId: String) = feedWorkerOneTimeRequest(feedId)
}
