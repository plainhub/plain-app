package com.ismartcoding.plain.db

import com.ismartcoding.plain.i18n.*

import android.content.ContentValues
import com.ismartcoding.plain.features.locale.LocaleHelper
import org.jetbrains.compose.resources.StringResource
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ismartcoding.lib.extensions.cut
import com.ismartcoding.lib.helpers.StringHelper
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.helpers.TimeHelper

class DataInitializer(val context: Context, val db: SupportSQLiteDatabase) {
    private data class TagItem(val nameKey: StringResource, val type: DataType)

    private data class MessageItem(val content: String, val fromId: String, val toId: String)

    private val now = TimeHelper.now().toString()

    private val tags =
        arrayOf(
            TagItem(Res.string.light_music, DataType.AUDIO),
            TagItem(Res.string.movie, DataType.VIDEO),
            TagItem(Res.string.family, DataType.IMAGE),
            TagItem(Res.string.important, DataType.SMS),
            TagItem(Res.string.todo, DataType.SMS),
            TagItem(Res.string.family, DataType.CONTACT),
            TagItem(Res.string.important, DataType.CONTACT),
            TagItem(Res.string.personal, DataType.NOTE),
            TagItem(Res.string.work, DataType.NOTE),
        )

    fun insertTags() {
        tags.forEach { tag ->
            db.insert(
                "tags",
                SQLiteDatabase.CONFLICT_NONE,
                ContentValues().apply {
                    put("id", StringHelper.shortUUID())
                    put("name", LocaleHelper.getStringSync(tag.nameKey))
                    put("type", tag.type.value)
                    put("count", 0)
                    put("created_at", now)
                    put("updated_at", now)
                },
            )
        }
    }

    fun insertNotes() {
        setOf(Res.string.note_sample1).forEach {
            val sample = LocaleHelper.getStringSync(it)
            db.insert(
                "notes",
                SQLiteDatabase.CONFLICT_NONE,
                ContentValues().apply {
                    put("id", StringHelper.shortUUID())
                    put("title", sample.cut(100).replace("\n", ""))
                    put("content", sample)
                    put("created_at", now)
                    put("updated_at", now)
                },
            )
        }
    }

    fun insertWelcome() {
        setOf<MessageItem>(
            MessageItem("""{"type":"text","value":{"text":"${LocaleHelper.getStringSync(Res.string.welcome_text)}"}}""", "local", "me"),
        ).forEach {
            db.insert(
                "chats",
                SQLiteDatabase.CONFLICT_NONE,
                ContentValues().apply {
                    put("id", StringHelper.shortUUID())
                    put("from_id", it.fromId)
                    put("to_id", it.toId)
                    put("channel_id", "") // Empty string for local chat (not a channel chat)
                    put("status", "sent") // Set status for welcome message
                    put("content", it.content)
                    put("created_at", now)
                    put("updated_at", now)
                },
            )
        }
    }
}
