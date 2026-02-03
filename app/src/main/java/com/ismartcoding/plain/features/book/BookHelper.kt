package com.ismartcoding.plain.features.book

import androidx.sqlite.db.SimpleSQLiteQuery
import com.ismartcoding.lib.content.ContentWhere
import com.ismartcoding.lib.helpers.SearchHelper
import com.ismartcoding.plain.db.*
import com.ismartcoding.plain.helpers.QueryHelper
import com.ismartcoding.plain.helpers.TimeHelper

object BookHelper {
    val bookDao: BookDao by lazy {
        AppDatabase.instance.bookDao()
    }

    suspend fun count(query: String): Int {
        var sql = "SELECT COUNT(id) FROM books"
        val where = ContentWhere()
        if (query.isNotEmpty()) {
            parseQuery(where, query)
            sql += " WHERE ${where.toSelection()}"
        }

        return bookDao.count(SimpleSQLiteQuery(sql, where.args.toTypedArray()))
    }

    suspend fun search(
        query: String,
        limit: Int,
        offset: Int,
    ): List<DBook> {
        var sql = "SELECT * FROM books"
        val where = ContentWhere()
        if (query.isNotEmpty()) {
            parseQuery(where, query)
            sql += " WHERE ${where.toSelection()}"
        }

        sql += " LIMIT $limit OFFSET $offset"

        return bookDao.search(SimpleSQLiteQuery(sql, where.args.toTypedArray()))
    }

    fun updateAsync(
        id: String,
        updateItem: DBook.() -> Unit,
    ): String {
        val item = bookDao.getById(id) ?: return id
        item.updatedAt = TimeHelper.now()
        updateItem(item)
        bookDao.update(item)

        return item.id
    }

    private suspend fun parseQuery(
        where: ContentWhere,
        query: String,
    ) {
        QueryHelper.parseAsync(query).forEach {
            if (it.name == "text") {
                where.addLikes(listOf("name", "description"), listOf(it.value, it.value))
            } else if (it.name == "ids") {
                where.addIn("id", it.value.split(","))
            }
        }
    }
}
