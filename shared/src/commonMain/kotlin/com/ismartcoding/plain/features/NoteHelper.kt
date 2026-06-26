package com.ismartcoding.plain.features

import com.ismartcoding.plain.helpers.ContentWhere
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.rawQuery
import com.ismartcoding.plain.db.DNote
import com.ismartcoding.plain.db.NoteDao
import com.ismartcoding.plain.helpers.QueryHelper
import com.ismartcoding.plain.helpers.TimeHelper

object NoteHelper {
    private val noteDao: NoteDao by lazy {
        AppDatabase.instance.noteDao()
    }

    suspend fun count(query: String): Int = withIO {
        var sql = "SELECT COUNT(id) FROM notes"
        val where = ContentWhere()
        parseQuery(where, query)
        sql += " WHERE ${where.toSelection()}"

        noteDao.count(rawQuery(sql, where.args.toTypedArray()))
    }

    suspend fun getIdsAsync(query: String): Set<String> = withIO {
        var sql = "SELECT id FROM notes"
        val where = ContentWhere()
        if (query.isNotEmpty()) {
            parseQuery(where, query)
            sql += " WHERE ${where.toSelection()}"
        }

        noteDao.getIds(rawQuery(sql, where.args.toTypedArray())).map { it.id }.toSet()
    }

    suspend fun getTrashedIdsAsync(query: String): Set<String> = withIO {
        var sql = "SELECT id FROM notes"
        val where = ContentWhere()
        where.trash = true
        if (query.isNotEmpty()) {
            parseQuery(where, query)
            sql += " WHERE ${where.toSelection()}"
        }

        return@withIO noteDao.getIds(rawQuery(sql, where.args.toTypedArray())).map { it.id }.toSet()
    }

    suspend fun search(
        query: String,
        limit: Int,
        offset: Int,
    ): List<DNote> = withIO {
        var sql = "SELECT * FROM notes"
        val where = ContentWhere()
        parseQuery(where, query)
        sql += " WHERE ${where.toSelection()}"

        sql += if (limit == Int.MAX_VALUE) {
            " ORDER BY updated_at DESC"
        } else {
            " ORDER BY updated_at DESC LIMIT $limit OFFSET $offset"
        }
        noteDao.search(rawQuery(sql, where.args.toTypedArray()))
    }

    suspend fun deleteAsync(query: String) = withIO {
        var sql = "DELETE FROM notes"
        val where = ContentWhere()
        if (query.isNotEmpty()) {
            parseQuery(where, query)
            sql += " WHERE ${where.toSelection()}"
        }

        noteDao.delete(rawQuery(sql, where.args.toTypedArray()))
    }

    suspend fun getById(id: String): DNote? = withIO {
        noteDao.getById(id)
    }

    suspend fun saveToNotesAsync(
        id: String,
        updateItem: DNote.() -> Unit,
    ): String = withIO {
        var item = noteDao.getById(id)
        var isInsert = false
        if (item == null) {
            item = DNote(id)
            isInsert = true
        } else {
            item.updatedAt = TimeHelper.now()
        }

        updateItem(item)

        if (isInsert) {
            noteDao.insert(item)
        } else {
            noteDao.update(item)
        }

        item.id
    }


    suspend fun addOrUpdateAsync(
        id: String,
        updateItem: DNote.() -> Unit,
    ): DNote = withIO {
        var item = if (id.isNotEmpty()) noteDao.getById(id) else null
        var isInsert = false
        if (item == null) {
            item = DNote()
            isInsert = true
        } else {
            item.updatedAt = TimeHelper.now()
        }

        updateItem(item)

        if (isInsert) {
            noteDao.insert(item)
        } else {
            noteDao.update(item)
        }

        item
    }

    suspend fun trashAsync(ids: Set<String>) = withIO {
        val now = TimeHelper.now()
        noteDao.trash(ids, now, now)
    }

    suspend fun restoreAsync(ids: Set<String>) = withIO {
        noteDao.trash(ids, null, TimeHelper.now())
    }

    suspend fun deleteAsync(ids: Set<String>) = withIO {
        noteDao.delete(ids)
    }

    private suspend fun parseQuery(
        where: ContentWhere,
        query: String,
    ) {
        QueryHelper.parseAsync(query).forEach {
            when (it.name) {
                "text" -> {
                    where.addLike("content", it.value)
                }

                "ids" -> {
                    where.addIn("id", it.value.split(","))
                }

                "trash" -> {
                    where.trash = it.value.toBooleanStrictOrNull()
                }
            }
        }
        if (where.trash == true) {
            where.add("deleted_at IS NOT NULL")
        } else {
            where.add("deleted_at IS NULL")
        }
    }
}
