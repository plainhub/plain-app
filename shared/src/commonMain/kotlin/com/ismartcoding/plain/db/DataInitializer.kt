package com.ismartcoding.plain.db

import androidx.sqlite.SQLiteConnection
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.helpers.StringHelper
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.family
import com.ismartcoding.plain.i18n.important
import com.ismartcoding.plain.i18n.light_music
import com.ismartcoding.plain.i18n.movie
import com.ismartcoding.plain.i18n.note_sample1
import com.ismartcoding.plain.i18n.personal
import com.ismartcoding.plain.i18n.todo
import com.ismartcoding.plain.i18n.welcome_text
import com.ismartcoding.plain.i18n.work
import com.ismartcoding.plain.lib.extensions.cut
import com.ismartcoding.plain.platform.LocaleHelper
import org.jetbrains.compose.resources.StringResource

/**
 * Seeds the database on first creation with welcome message, default tags, and
 * a sample note. Uses the KMP [SQLiteConnection] API (prepared statements)
 * instead of Android's `ContentValues` so the same initializer runs on both
 * Android and iOS.
 */
class DataInitializer(private val connection: SQLiteConnection) {
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
        val stmt = connection.prepare(
            "INSERT INTO tags (id, name, type, count, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
        )
        try {
            tags.forEach { tag ->
                stmt.bindText(1, StringHelper.shortUUID())
                stmt.bindText(2, LocaleHelper.getString(tag.nameKey))
                stmt.bindLong(3, tag.type.value.toLong())
                stmt.bindLong(4, 0L)
                stmt.bindText(5, now)
                stmt.bindText(6, now)
                stmt.step()
                stmt.reset()
            }
        } finally {
            stmt.close()
        }
    }

    fun insertNotes() {
        val stmt = connection.prepare(
            "INSERT INTO notes (id, title, content, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
        )
        try {
            setOf(Res.string.note_sample1).forEach {
                val sample = LocaleHelper.getString(it)
                stmt.bindText(1, StringHelper.shortUUID())
                stmt.bindText(2, sample.cut(100).replace("\n", ""))
                stmt.bindText(3, sample)
                stmt.bindText(4, now)
                stmt.bindText(5, now)
                stmt.step()
                stmt.reset()
            }
        } finally {
            stmt.close()
        }
    }

    fun insertWelcome() {
        val stmt = connection.prepare(
            "INSERT INTO chats (id, from_id, to_id, channel_id, status, content, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
        )
        try {
            setOf(
                MessageItem(
                    """{"type":"text","value":{"text":"${LocaleHelper.getString(Res.string.welcome_text)}"}}""",
                    "local",
                    "me",
                ),
            ).forEach {
                stmt.bindText(1, StringHelper.shortUUID())
                stmt.bindText(2, it.fromId)
                stmt.bindText(3, it.toId)
                stmt.bindText(4, "") // Empty string for local chat (not a channel chat)
                stmt.bindText(5, "sent") // Set status for welcome message
                stmt.bindText(6, it.content)
                stmt.bindText(7, now)
                stmt.bindText(8, now)
                stmt.step()
                stmt.reset()
            }
        } finally {
            stmt.close()
        }
    }
}
