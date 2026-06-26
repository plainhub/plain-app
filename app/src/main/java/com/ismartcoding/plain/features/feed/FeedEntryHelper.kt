package com.ismartcoding.plain.features.feed

import com.ismartcoding.plain.helpers.ContentWhere
import com.ismartcoding.plain.db.rawQuery
import com.ismartcoding.plain.lib.helpers.CoroutinesHelper.withIO
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

    suspend fun count(query: String): Int = withIO {
        var sql = "SELECT COUNT(id) FROM feed_entries"
        val where = ContentWhere()
        if (query.isNotEmpty()) {
            parseQuery(where, query)
            sql += " WHERE ${where.toSelection()}"
        }

        feedEntryDao.count(rawQuery(sql, where.args.toTypedArray()))
    }

    suspend fun getIdsAsync(query: String): Set<String> = withIO {
        var sql = "SELECT id FROM feed_entries"
        val where = ContentWhere()
        if (query.isNotEmpty()) {
            parseQuery(where, query)
            sql += " WHERE ${where.toSelection()}"
        }

        feedEntryDao.getIds(rawQuery(sql, where.args.toTypedArray())).map { it.id }.toSet()
    }

    suspend fun search(
        query: String,
        limit: Int,
        offset: Int,
    ): List<DFeedEntry> = withIO {
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

        feedEntryDao.search(rawQuery(sql, where.args.toTypedArray()))
    }

    suspend fun getAsync(id: String): DFeedEntry? = withIO {
        feedEntryDao.getById(id)
    }

    suspend fun updateAsync(
        id: String,
        updateItem: DFeedEntry.() -> Unit,
    ): String = withIO {
        val item = feedEntryDao.getById(id) ?: return@withIO id
        item.updatedAt = TimeHelper.now()
        updateItem(item)
        feedEntryDao.update(item)

        item.id
    }

    suspend fun updateAsync(
        item: DFeedEntry,
    ) = withIO {
        item.updatedAt = TimeHelper.now()
        feedEntryDao.update(item)
    }

    suspend fun deleteAsync(ids: Set<String>) = withIO {
        ids.chunked(50).forEach { chunk ->
            feedEntryDao.delete(chunk.toSet())
        }
    }

    suspend fun deleteAllAsync() = withIO {
        feedEntryDao.deleteAll()
    }

    private suspend fun parseQuery(
        where: ContentWhere,
        query: String,
    ) = withIO {
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
