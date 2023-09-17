package com.ismartcoding.plain.features.note

import androidx.sqlite.db.SimpleSQLiteQuery
import com.ismartcoding.lib.content.ContentWhere
import com.ismartcoding.lib.helpers.SearchHelper
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DNote
import com.ismartcoding.plain.db.NoteDao
import kotlinx.datetime.Clock

object NoteHelper {
    private val noteDao: NoteDao by lazy {
        AppDatabase.instance.noteDao()
    }

    fun count(query: String): Int {
        var sql = "SELECT COUNT(id) FROM notes"
        val where = ContentWhere()
        if (query.isNotEmpty()) {
            parseQuery(where, query)
            sql += " WHERE ${where.toSelection()}"
        }

        return noteDao.count(SimpleSQLiteQuery(sql, where.args.toTypedArray()))
    }

    fun getIdsAsync(query: String): Set<String> {
        var sql = "SELECT id FROM notes"
        val where = ContentWhere()
        if (query.isNotEmpty()) {
            parseQuery(where, query)
            sql += " WHERE ${where.toSelection()}"
        }

        return noteDao.getIds(SimpleSQLiteQuery(sql, where.args.toTypedArray())).map { it.id }.toSet()
    }

    fun search(query: String, limit: Int, offset: Int): List<DNote> {
        var sql = "SELECT * FROM notes"
        val where = ContentWhere()
        if (query.isNotEmpty()) {
            parseQuery(where, query)
            sql += " WHERE ${where.toSelection()}"
        }

        sql += " ORDER BY updated_at DESC LIMIT $limit OFFSET $offset"

        return noteDao.search(SimpleSQLiteQuery(sql, where.args.toTypedArray()))
    }


    fun deleteAsync(query: String) {
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

    fun addOrUpdateAsync(id: String, updateItem: DNote.() -> Unit): String {
        var item = if (id.isNotEmpty()) noteDao.getById(id) else null
        var isInsert = false
        if (item == null) {
            item = DNote()
            isInsert = true
        } else {
            item.updatedAt = Clock.System.now()
        }

        updateItem(item)

        if (isInsert) {
            noteDao.insert(item)
        } else {
            noteDao.update(item)
        }

        return item.id
    }

    fun trashAsync(ids: Set<String>) {
        noteDao.trash(ids, Clock.System.now())
    }

    fun untrashAsync(ids: Set<String>) {
        noteDao.trash(ids, null)
    }

    fun deleteAsync(ids: Set<String>) {
        noteDao.delete(ids)
    }

    private fun parseQuery(where: ContentWhere, query: String) {
        val queryGroups = SearchHelper.parse(query)
        queryGroups.forEach {
            if (it.name == "text") {
                where.addLike("content", it.value)
            } else if (it.name == "ids") {
                val ids = it.value.split(",")
                if (ids.isNotEmpty()) {
                    where.addIn("id", ids)
                }
            } else if (it.name == "trash") {
                if (it.value == "true") {
                    where.add("deleted_at IS NOT NULL")
                } else {
                    where.add("deleted_at IS NULL")
                }
            }
        }
    }
}