package com.ismartcoding.plain.features.feed

import androidx.sqlite.db.SimpleSQLiteQuery
import com.ismartcoding.lib.content.ContentWhere
import com.ismartcoding.lib.helpers.SearchHelper
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DFeedEntry
import com.ismartcoding.plain.db.FeedEntryDao
import kotlinx.datetime.Clock

object FeedEntryHelper {
    val feedEntryDao: FeedEntryDao by lazy {
        AppDatabase.instance.feedEntryDao()
    }

    fun count(query: String): Int {
        var sql = "SELECT COUNT(id) FROM feed_entries"
        val where = ContentWhere()
        if (query.isNotEmpty()) {
            parseQuery(where, query)
            sql += " WHERE ${where.toSelection()}"
        }

        return feedEntryDao.count(SimpleSQLiteQuery(sql, where.args.toTypedArray()))
    }

    fun getIdsAsync(query: String): Set<String> {
        var sql = "SELECT id FROM feed_entries"
        val where = ContentWhere()
        if (query.isNotEmpty()) {
            parseQuery(where, query)
            sql += " WHERE ${where.toSelection()}"
        }

        return feedEntryDao.getIds(SimpleSQLiteQuery(sql, where.args.toTypedArray())).map { it.id }.toSet()
    }

    fun search(
        query: String,
        limit: Int,
        offset: Int,
    ): List<DFeedEntry> {
        var sql = "SELECT * FROM feed_entries"
        val where = ContentWhere()
        if (query.isNotEmpty()) {
            parseQuery(where, query)
            sql += " WHERE ${where.toSelection()}"
        }

        sql += " ORDER BY published_at DESC LIMIT $limit OFFSET $offset"

        return feedEntryDao.search(SimpleSQLiteQuery(sql, where.args.toTypedArray()))
    }

    fun updateAsync(
        id: String,
        updateItem: DFeedEntry.() -> Unit,
    ): String {
        val item = feedEntryDao.getById(id) ?: return id
        item.updatedAt = Clock.System.now()
        updateItem(item)
        feedEntryDao.update(item)

        return item.id
    }

    fun deleteAsync(ids: Set<String>) {
        feedEntryDao.delete(ids)
    }

    private fun parseQuery(
        where: ContentWhere,
        query: String,
    ) {
        val queryGroups = SearchHelper.parse(query)
        queryGroups.forEach {
            if (it.name == "text") {
                where.addLikes(listOf("description", "content"), listOf(it.value, it.value))
            } else if (it.name == "feed_id") {
                where.add("feed_id=?", it.value)
            } else if (it.name == "ids") {
                val ids = it.value.split(",")
                if (ids.isNotEmpty()) {
                    where.addIn("id", ids)
                }
            }
        }
    }
}
