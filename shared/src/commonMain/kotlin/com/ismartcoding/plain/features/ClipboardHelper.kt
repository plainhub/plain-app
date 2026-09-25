package com.ismartcoding.plain.features

import com.ismartcoding.plain.db.DClipboard
import com.ismartcoding.plain.db.rawQuery
import com.ismartcoding.plain.helpers.ContentWhere
import com.ismartcoding.plain.helpers.FilterField
import com.ismartcoding.plain.helpers.QueryHelper
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.AppDatabase

object ClipboardHelper {
    private val dao by lazy {
        AppDatabase.instance.clipboardDao()
    }

    suspend fun getPage(limit: Int, offset: Int, query: String = ""): List<DClipboard> = withIO {
        var sql = "SELECT * FROM clipboards"
        val where = ContentWhere()
        applyClipboardSearch(where, query)
        sql += " WHERE ${where.toSelection()} ORDER BY created_at DESC LIMIT $limit OFFSET $offset"
        dao.search(rawQuery(sql, where.args.toTypedArray()))
    }

    suspend fun count(query: String = ""): Int = withIO {
        var sql = "SELECT COUNT(*) FROM clipboards"
        val where = ContentWhere()
        applyClipboardSearch(where, query)
        sql += " WHERE ${where.toSelection()}"
        dao.count(rawQuery(sql, where.args.toTypedArray()))
    }

    /** History list search: literal match across text/label/source (escaping handled by ContentWhere.addLikes). */
    internal fun applyClipboardSearch(where: ContentWhere, query: String) {
        if (query.isNotEmpty()) {
            where.addLikes(listOf("text", "label", "source"), listOf(query, query, query))
        }
    }

    suspend fun getLatestByHash(hash: String): DClipboard? = withIO {
        dao.getLatestByHash(hash)
    }

    suspend fun insert(entry: DClipboard) = withIO {
        dao.insert(entry)
    }

    suspend fun deleteByIds(ids: List<String>): Int = withIO {
        dao.deleteByIds(ids)
    }

    /** Resolves the query DSL to matching entry ids (NoteHelper.getIdsAsync pattern); the caller deletes via the parameterized deleteByIds. */
    suspend fun getIdsAsync(query: String): Set<String> = withIO {
        var sql = "SELECT id FROM clipboards"
        val where = ContentWhere()
        if (query.isNotEmpty()) {
            applyClipboardFilterFields(where, QueryHelper.parseAsync(query))
            sql += " WHERE ${where.toSelection()}"
        }

        dao.getIds(rawQuery(sql, where.args.toTypedArray())).map { it.id }.toSet()
    }

    /** Pure field-application seam (host-testable): parsed query fields → where conditions. */
    internal fun applyClipboardFilterFields(where: ContentWhere, fields: List<FilterField>) {
        fields.forEach {
            when (it.name) {
                QueryHelper.BULK_ALL_FIELD -> {} // explicit whole-table sentinel — no condition
                "text" -> {
                    where.addLike("text", it.value)
                }

                "ids" -> {
                    where.addIn("id", it.value.split(","))
                }
            }
        }
    }

    suspend fun clear() = withIO {
        dao.clear()
    }

    /** Resolves a source client id to the peer name; empty source = this device. */
    suspend fun getSourceName(source: String): String = withIO {
        if (source.isBlank()) "" else AppDatabase.instance.peerDao().getById(source)?.name ?: source
    }
}
