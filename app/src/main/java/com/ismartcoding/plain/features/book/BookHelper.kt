package com.ismartcoding.plain.features.book

import androidx.sqlite.db.SimpleSQLiteQuery
import com.ismartcoding.lib.content.ContentWhere
import com.ismartcoding.lib.helpers.SearchHelper
import com.ismartcoding.plain.db.*
import kotlinx.datetime.Clock

object BookHelper {
    val bookDao: BookDao by lazy {
        AppDatabase.instance.bookDao()
    }

    fun count(query: String): Int {
        var sql = "SELECT COUNT(id) FROM books"
        val where = ContentWhere()
        if (query.isNotEmpty()) {
            parseQuery(where, query)
            sql += " WHERE ${where.toSelection()}"
        }

        return bookDao.count(SimpleSQLiteQuery(sql, where.args.toTypedArray()))
    }

    fun search(
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
        item.updatedAt = Clock.System.now()
        updateItem(item)
        bookDao.update(item)

        return item.id
    }

    private fun parseQuery(
        where: ContentWhere,
        query: String,
    ) {
        val queryGroups = SearchHelper.parse(query)
        queryGroups.forEach {
            if (it.name == "text") {
                where.addLikes(listOf("name", "description"), listOf(it.value, it.value))
            } else if (it.name == "ids") {
                val ids = it.value.split(",")
                if (ids.isNotEmpty()) {
                    where.addIn("id", ids)
                }
            }
        }
    }
}
