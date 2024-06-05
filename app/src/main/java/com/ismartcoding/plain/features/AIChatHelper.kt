package com.ismartcoding.plain.features

import androidx.sqlite.db.SimpleSQLiteQuery
import com.ismartcoding.lib.content.ContentSort
import com.ismartcoding.lib.content.ContentWhere
import com.ismartcoding.plain.db.AIChatDao
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DAIChat
import com.ismartcoding.plain.helpers.QueryHelper

object AIChatHelper {
    private val chatDao: AIChatDao by lazy {
        AppDatabase.instance.aiChatDao()
    }

    suspend fun getChatsAsync(id: String): List<DAIChat> {
        return chatDao.getChats(id)
    }

    suspend fun createChatItemsAsync(
        parentId: String,
        isMe: Boolean,
        message: String,
    ): List<DAIChat> {
        val item = DAIChat()
        item.isMe = isMe
        item.content = message
        item.parentId = parentId
        chatDao.insert(item)
        return listOf(item)
    }

    suspend fun countAsync(query: String): Int {
        var sql = "SELECT COUNT(id) FROM aichats"
        val where = ContentWhere()
        val sort = ContentSort("updated_at", "DESC")
        if (query.isNotEmpty()) {
            parseQueryAsync(where, query, sort)
            sql += " WHERE ${where.toSelection()}"
        }

        return chatDao.count(SimpleSQLiteQuery(sql, where.args.toTypedArray()))
    }

    suspend fun getIdsAsync(query: String): Set<String> {
        var sql = "SELECT id FROM aichats"
        val where = ContentWhere()
        if (query.isNotEmpty()) {
            parseQueryAsync(where, query)
            sql += " WHERE ${where.toSelection()}"
        }

        return chatDao.getIds(SimpleSQLiteQuery(sql, where.args.toTypedArray())).map { it.id }.toSet()
    }

    suspend fun searchAsync(
        query: String,
        limit: Int,
        offset: Int,
    ): List<DAIChat> {
        var sql = "SELECT * FROM aichats"
        val where = ContentWhere()
        val sort = ContentSort("updated_at", "DESC")
        if (query.isNotEmpty()) {
            parseQueryAsync(where, query, sort)
            sql += " WHERE ${where.toSelection()}"
        }

        sql += " ORDER BY ${sort.name} ${sort.direction} LIMIT $limit OFFSET $offset"

        return chatDao.search(SimpleSQLiteQuery(sql, where.args.toTypedArray()))
    }

    suspend fun getAsync(id: String): DAIChat? {
        return chatDao.getById(id)
    }

    suspend fun deleteAsync(ids: Set<String>) {
        chatDao.delete(ids)
    }

    suspend fun deleteAsync(query: String) {
        var sql = "DELETE FROM aichats"
        val where = ContentWhere()
        if (query.isNotEmpty()) {
            parseQueryAsync(where, query)
            sql += " WHERE ${where.toSelection()}"
        }

        chatDao.delete(SimpleSQLiteQuery(sql, where.args.toTypedArray()))
    }

    private suspend fun parseQueryAsync(
        where: ContentWhere,
        query: String,
        sort: ContentSort? = null,
    ) {
        QueryHelper.parseAsync(query).forEach {
            when (it.name) {
                "text" -> {
                    where.addLikes(listOf("content"), listOf(it.value, it.value))
                }

                "parent_id" -> {
                    where.add("parent_id=?", it.value)
                }

                "ids" -> {
                    where.addIn("id", it.value.split(","))
                }

                "parent_ids" -> {
                    where.addIn("parent_id", it.value.split(","))
                }

                "sort" -> {
                    val split = it.value.split("-")
                    sort?.name = split[0]
                    sort?.direction = split[1]
                }
            }
        }
    }
}
