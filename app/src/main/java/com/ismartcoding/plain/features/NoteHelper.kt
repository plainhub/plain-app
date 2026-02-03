package com.ismartcoding.plain.features

import androidx.sqlite.db.SimpleSQLiteQuery
import com.ismartcoding.lib.content.ContentWhere
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DNote
import com.ismartcoding.plain.db.NoteDao
import com.ismartcoding.plain.helpers.QueryHelper
import com.ismartcoding.plain.helpers.TimeHelper

object NoteHelper {
    private val noteDao: NoteDao by lazy {
        AppDatabase.instance.noteDao()
    }

    suspend fun count(query: String): Int {
        var sql = "SELECT COUNT(id) FROM notes"
        val where = ContentWhere()
        parseQuery(where, query)
        sql += " WHERE ${where.toSelection()}"

        return noteDao.count(SimpleSQLiteQuery(sql, where.args.toTypedArray()))
    }

    suspend fun getIdsAsync(query: String): Set<String> {
        var sql = "SELECT id FROM notes"
        val where = ContentWhere()
        if (query.isNotEmpty()) {
            parseQuery(where, query)
            sql += " WHERE ${where.toSelection()}"
        }

        return noteDao.getIds(SimpleSQLiteQuery(sql, where.args.toTypedArray())).map { it.id }.toSet()
    }

    suspend fun getTrashedIdsAsync(query: String): Set<String> {
        var sql = "SELECT id FROM notes"
        val where = ContentWhere()
        where.trash = true
        if (query.isNotEmpty()) {
            parseQuery(where, query)
            sql += " WHERE ${where.toSelection()}"
        }

        return noteDao.getIds(SimpleSQLiteQuery(sql, where.args.toTypedArray())).map { it.id }.toSet()
    }

    suspend fun search(
        query: String,
        limit: Int,
        offset: Int,
    ): List<DNote> {
        var sql = "SELECT * FROM notes"
        val where = ContentWhere()
        parseQuery(where, query)
        sql += " WHERE ${where.toSelection()}"

        sql += if (limit == Int.MAX_VALUE) {
            " ORDER BY updated_at DESC"
        } else {
            " ORDER BY updated_at DESC LIMIT $limit OFFSET $offset"
        }
        return noteDao.search(SimpleSQLiteQuery(sql, where.args.toTypedArray()))
    }

    suspend fun deleteAsync(query: String) {
        var sql = "DELETE FROM notes"
        val where = ContentWhere()
        if (query.isNotEmpty()) {
            parseQuery(where, query)
            sql += " WHERE ${where.toSelection()}"
        }

        noteDao.delete(SimpleSQLiteQuery(sql, where.args.toTypedArray()))
    }

    fun getById(id: String): DNote? {
        return noteDao.getById(id)
    }

    fun saveToNotesAsync(
        id: String,
        updateItem: DNote.() -> Unit,
    ): String {
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

        return item.id
    }


    fun addOrUpdateAsync(
        id: String,
        updateItem: DNote.() -> Unit,
    ): DNote {
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

        return item
    }

    fun trashAsync(ids: Set<String>) {
        val now = TimeHelper.now()
        noteDao.trash(ids, now, now)
    }

    fun restoreAsync(ids: Set<String>) {
        noteDao.trash(ids, null, TimeHelper.now())
    }

    fun deleteAsync(ids: Set<String>) {
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
