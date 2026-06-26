package com.ismartcoding.plain.features.book

import com.ismartcoding.plain.helpers.ContentWhere
import com.ismartcoding.plain.db.rawQuery
import com.ismartcoding.plain.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.db.*
import com.ismartcoding.plain.helpers.QueryHelper

object BookHelper {
    val bookDao: BookDao by lazy {
        AppDatabase.instance.bookDao()
    }

    val bookChapterDao: BookChapterDao by lazy {
        AppDatabase.instance.bookChapterDao()
    }

    suspend fun count(query: String): Int = withIO {
        var sql = "SELECT COUNT(id) FROM books"
        val where = ContentWhere()
        if (query.isNotEmpty()) {
            parseQuery(where, query)
            sql += " WHERE ${where.toSelection()}"
        }

        bookDao.count(rawQuery(sql, where.args.toTypedArray()))
    }

    suspend fun search(
        query: String,
        limit: Int,
        offset: Int,
    ): List<DBook> = withIO {
        var sql = "SELECT * FROM books"
        val where = ContentWhere()
        if (query.isNotEmpty()) {
            parseQuery(where, query)
            sql += " WHERE ${where.toSelection()}"
        }

        sql += " LIMIT $limit OFFSET $offset"

        bookDao.search(rawQuery(sql, where.args.toTypedArray()))
    }

    suspend fun getById(id: String): DBook? = withIO {
        bookDao.getById(id)
    }

    suspend fun getAll(): List<DBook> = withIO {
        bookDao.getAll()
    }

    suspend fun addAsync(book: DBook) = withIO {
        bookDao.insert(book)
    }

    suspend fun updateAsync(book: DBook) = withIO {
        bookDao.update(book)
    }

    suspend fun deleteAsync(ids: Set<String>) = withIO {
        bookDao.delete(ids)
    }

    suspend fun getChaptersAsync(bookId: String): List<DBookChapter> = withIO {
        bookChapterDao.getAll(bookId)
    }

    suspend fun getChapter(id: String): DBookChapter? = withIO {
        bookChapterDao.getById(id)
    }

    suspend fun updateChapter(chapter: DBookChapter) = withIO {
        bookChapterDao.update(chapter)
    }

    suspend fun addChapter(chapter: DBookChapter) = withIO {
        bookChapterDao.insert(chapter)
    }

    suspend fun deleteChapters(ids: Set<String>) = withIO {
        bookChapterDao.delete(ids)
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
