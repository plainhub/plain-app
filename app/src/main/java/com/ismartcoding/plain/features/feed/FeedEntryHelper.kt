package com.ismartcoding.plain.features.feed

import androidx.sqlite.db.SimpleSQLiteQuery
import com.ismartcoding.lib.content.ContentWhere
import com.ismartcoding.lib.helpers.SearchHelper
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DFeedEntry
import com.ismartcoding.plain.db.FeedEntryDao
import com.ismartcoding.plain.helpers.QueryHelper
import com.ismartcoding.plain.helpers.TimeHelper
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

object FeedEntryHelper {
    val feedEntryDao: FeedEntryDao by lazy {
        AppDatabase.instance.feedEntryDao()
    }

    suspend fun count(query: String): Int {
        var sql = "SELECT COUNT(id) FROM feed_entries"
        val where = ContentWhere()
        if (query.isNotEmpty()) {
            parseQuery(where, query)
            sql += " WHERE ${where.toSelection()}"
        }

        return feedEntryDao.count(SimpleSQLiteQuery(sql, where.args.toTypedArray()))
    }

    suspend fun getIdsAsync(query: String): Set<String> {
        var sql = "SELECT id FROM feed_entries"
        val where = ContentWhere()
        if (query.isNotEmpty()) {
            parseQuery(where, query)
            sql += " WHERE ${where.toSelection()}"
        }

        return feedEntryDao.getIds(SimpleSQLiteQuery(sql, where.args.toTypedArray())).map { it.id }.toSet()
    }

    suspend fun search(
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

        sql += if (limit == Int.MAX_VALUE) {
            " ORDER BY published_at DESC"
        } else {
            " ORDER BY published_at DESC LIMIT $limit OFFSET $offset"
        }

        return feedEntryDao.search(SimpleSQLiteQuery(sql, where.args.toTypedArray()))
    }

    fun getAsync(id: String): DFeedEntry? {
        return feedEntryDao.getById(id)
    }

    fun updateAsync(
        id: String,
        updateItem: DFeedEntry.() -> Unit,
    ): String {
        val item = feedEntryDao.getById(id) ?: return id
        item.updatedAt = TimeHelper.now()
        updateItem(item)
        feedEntryDao.update(item)

        return item.id
    }

    fun updateAsync(
        item: DFeedEntry,
    ) {
        item.updatedAt = TimeHelper.now()
        feedEntryDao.update(item)
    }

    fun deleteAsync(ids: Set<String>) {
        ids.chunked(50).forEach { chunk ->
            feedEntryDao.delete(chunk.toSet())
        }
    }

    fun deleteAllAsync() {
        feedEntryDao.deleteAll()
    }

    private suspend fun parseQuery(
        where: ContentWhere,
        query: String,
    ) {
        QueryHelper.parseAsync(query).forEach {
            if (it.name == "text") {
                where.addLikes(listOf("title", "description", "content"), listOf(it.value, it.value, it.value))
            } else if (it.name == "feed_id") {
                where.add("feed_id=?", it.value)
            } else if (it.name == "today" && it.value == "true") {
                val currentDateTime = TimeHelper.now()
                val timeZone = TimeZone.currentSystemDefault()
                val startOfDay = currentDateTime.toLocalDateTime(timeZone)
                    .date
                    .atStartOfDayIn(timeZone)
                where.add("published_at>=?", startOfDay.toString())
            } else if (it.name == "ids") {
                where.addIn("id", it.value.split(","))
            } else if (it.name == "created_at") {
                where.add("created_at ${it.op} ?", it.value)
            }
        }
    }
}
