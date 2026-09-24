package com.ismartcoding.plain.db

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Manual Room migrations for [com.ismartcoding.plain.platform.AppDatabase].
 *
 * All auto-migrations are declared directly on the `@Database` annotation; only
 * the 5→6 migration (which rewrites the chats table and adds peers/chat_groups)
 * needs manual SQL. Uses the KMP [SQLiteConnection] API so the same migration
 * runs on both Android and iOS.
 */
object Migrations {
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override suspend fun migrate(connection: SQLiteConnection) {
            // Ensure pomodoro_items exists in case it was missing from a prior incomplete migration
            connection.execSQL("""
                CREATE TABLE IF NOT EXISTS `pomodoro_items` (
                    `id` TEXT NOT NULL,
                    `date` TEXT NOT NULL,
                    `completed_count` INTEGER NOT NULL,
                    `total_work_seconds` INTEGER NOT NULL,
                    `total_break_seconds` INTEGER NOT NULL,
                    `created_at` TEXT NOT NULL,
                    `updated_at` TEXT NOT NULL,
                    PRIMARY KEY(`id`)
                )
            """)

            // Create new table with desired structure
            connection.execSQL("""
                CREATE TABLE chats_new (
                    id TEXT PRIMARY KEY NOT NULL,
                    from_id TEXT NOT NULL,
                    to_id TEXT NOT NULL,
                    group_id TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'PENDING',
                    content TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL
                )
            """)

            // Copy and transform data
            connection.execSQL("""
                INSERT INTO chats_new (id, from_id, to_id, group_id, status, content, created_at, updated_at)
                SELECT id,
                       CASE WHEN is_me = 1 THEN 'me' ELSE 'local' END as from_id,
                       CASE WHEN is_me = 1 THEN 'local' ELSE 'me' END as to_id,
                       '',
                       'sent',
                       content, created_at, updated_at
                FROM chats
            """)

            // Create new tables
            connection.execSQL("""
                CREATE TABLE peers (
                    id TEXT PRIMARY KEY NOT NULL,
                    name TEXT NOT NULL,
                    ip TEXT NOT NULL,
                    key TEXT NOT NULL,
                    public_key TEXT NOT NULL,
                    status TEXT NOT NULL,
                    port INTEGER NOT NULL,
                    device_type TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL
                )
            """)

            connection.execSQL("""
                CREATE TABLE chat_groups (
                    id TEXT PRIMARY KEY NOT NULL,
                    name TEXT NOT NULL,
                    key TEXT NOT NULL,
                    members TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL
                )
            """)

            // Replace old table
            connection.execSQL("DROP TABLE chats")
            connection.execSQL("ALTER TABLE chats_new RENAME TO chats")

            // Create indexes for chats table
            connection.execSQL("CREATE INDEX index_chats_from_id ON chats(from_id)")
            connection.execSQL("CREATE INDEX index_chats_to_id ON chats(to_id)")
            connection.execSQL("CREATE INDEX index_chats_group_id ON chats(group_id)")
        }
    }

    val MIGRATION_18_19 = object : Migration(18, 19) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL("UPDATE peers SET status = UPPER(status) WHERE status != ''")
            connection.execSQL("UPDATE peers SET device_type = UPPER(device_type) WHERE device_type != ''")
        }
    }

    val MIGRATION_19_20 = object : Migration(19, 20) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL("UPDATE chat_channels SET status = UPPER(status) WHERE status != ''")
            connection.execSQL("UPDATE chats SET status = UPPER(status) WHERE status != ''")
            connection.execSQL("UPDATE sessions SET type = UPPER(type) WHERE type != ''")
            connection.execSQL(
                """
                UPDATE chat_channels
                SET members = REPLACE(
                    REPLACE(members, '"status":"joined"', '"status":"JOINED"'),
                    '"status":"pending"', '"status":"PENDING"'
                )
                WHERE members LIKE '%status%'
                """.trimIndent()
            )
        }
    }

    val MIGRATION_20_21 = object : Migration(20, 21) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                UPDATE chats
                SET content = REPLACE(
                    REPLACE(REPLACE(content, '"type":"text"', '"type":"TEXT"'), '"type":"images"', '"type":"IMAGES"'),
                    '"type":"files"', '"type":"FILES"'
                )
                WHERE content LIKE '%type%'
                """.trimIndent()
            )
        }
    }

    val MIGRATION_21_22 = object : Migration(21, 22) {
        override suspend fun migrate(connection: SQLiteConnection) {
            // Clean up invalid data that could cause Enum parsing crashes
            connection.execSQL("UPDATE chats SET status = 'PENDING' WHERE status = '' OR status IS NULL")
            connection.execSQL("UPDATE sessions SET type = 'WEB' WHERE type = '' OR type IS NULL")
            connection.execSQL("UPDATE chat_channels SET status = 'JOINED' WHERE status = '' OR status IS NULL")
            connection.execSQL("UPDATE peers SET status = 'UNPAIRED' WHERE status = '' OR status IS NULL")
            connection.execSQL("UPDATE peers SET device_type = 'OTHER' WHERE device_type = '' OR device_type IS NULL")

            // Fix chats.status DEFAULT 'PENDING' and chats.status_data DEFAULT ''
            connection.execSQL(
                """
                CREATE TABLE chats_new (
                    id TEXT NOT NULL,
                    from_id TEXT NOT NULL,
                    to_id TEXT NOT NULL,
                    channel_id TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'PENDING',
                    status_data TEXT NOT NULL DEFAULT '',
                    content TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL,
                    PRIMARY KEY(id)
                )
                """
            )
            connection.execSQL(
                """
                INSERT INTO chats_new (id, from_id, to_id, channel_id, status, status_data, content, created_at, updated_at)
                SELECT id, from_id, to_id, channel_id, status, status_data, content, created_at, updated_at
                FROM chats
                """
            )
            connection.execSQL("DROP TABLE chats")
            connection.execSQL("ALTER TABLE chats_new RENAME TO chats")
            connection.execSQL("CREATE INDEX index_chats_from_id ON chats(from_id)")
            connection.execSQL("CREATE INDEX index_chats_to_id ON chats(to_id)")
            connection.execSQL("CREATE INDEX index_chats_channel_id ON chats(channel_id)")

            // Fix sessions.type DEFAULT 'WEB' (was 'web' before auto-migration 19→20)
            connection.execSQL(
                """
                CREATE TABLE sessions_new (
                    client_id TEXT NOT NULL,
                    name TEXT NOT NULL DEFAULT '',
                    type TEXT NOT NULL DEFAULT 'WEB',
                    client_ip TEXT NOT NULL,
                    os_name TEXT NOT NULL,
                    os_version TEXT NOT NULL,
                    browser_name TEXT NOT NULL,
                    browser_version TEXT NOT NULL,
                    token TEXT NOT NULL,
                    last_active_at TEXT,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL,
                    PRIMARY KEY(client_id)
                )
                """
            )
            connection.execSQL(
                """
                INSERT INTO sessions_new (client_id, name, type, client_ip, os_name, os_version, browser_name, browser_version, token, last_active_at, created_at, updated_at)
                SELECT client_id, name, type, client_ip, os_name, os_version, browser_name, browser_version, token, last_active_at, created_at, updated_at
                FROM sessions
                """
            )
            connection.execSQL("DROP TABLE sessions")
            connection.execSQL("ALTER TABLE sessions_new RENAME TO sessions")

            // Fix chat_channels.status DEFAULT 'JOINED' (was 'joined' before auto-migration 19→20)
            connection.execSQL(
                """
                CREATE TABLE chat_channels_new (
                    id TEXT NOT NULL,
                    name TEXT NOT NULL,
                    key TEXT NOT NULL,
                    owner TEXT NOT NULL DEFAULT '',
                    members TEXT NOT NULL,
                    version INTEGER NOT NULL DEFAULT 0,
                    status TEXT NOT NULL DEFAULT 'JOINED',
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL,
                    PRIMARY KEY(id)
                )
                """
            )
            connection.execSQL(
                """
                INSERT INTO chat_channels_new (id, name, key, owner, members, version, status, created_at, updated_at)
                SELECT id, name, key, owner, members, version, status, created_at, updated_at
                FROM chat_channels
                """
            )
            connection.execSQL("DROP TABLE chat_channels")
            connection.execSQL("ALTER TABLE chat_channels_new RENAME TO chat_channels")
        }
    }

    val MIGRATION_22_23 = object : Migration(22, 23) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `shares` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `password` TEXT NOT NULL,
                    `url_token` TEXT NOT NULL,
                    `expires_at` TEXT,
                    `read_only` INTEGER NOT NULL,
                    `data` TEXT NOT NULL,
                    `created_at` TEXT NOT NULL,
                    `updated_at` TEXT NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """
            )
        }
    }

    /**
     * 2026-09-24 naming/unit cleanup (all folded into one migration — v31 never
     * shipped to users; dev devices that already applied the interim 30→31 must
     * clear app data once):
     * - `chat_channels.owner` → `owner_id`; members JSON key `id` → `peerId`
     *   (legacy `id` keys stay decodable via `@JsonNames`).
     * - `video_play_progress.duration` → `position_ms` (it stores the playback
     *   position, not a duration).
     * - `archived_conversations.conversation_date`: epoch-millis INTEGER →
     *   ISO-8601 TEXT (Instant).
     * - audio/media duration columns `duration` → `duration_ms` with values
     *   converted seconds → milliseconds (unit alignment with MediaStore and
     *   the GraphQL `durationMs` contract): audio_queue_items,
     *   audio_playlist_items, audio_play_history, media_item.
     * - `feeds.entryCount` is an @Ignore in-memory field — no column, no migration.
     * - table `clipboard` → `clipboards` (plural, like every other table).
     */
    val MIGRATION_30_31 = object : Migration(30, 31) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE chat_channels RENAME COLUMN owner TO owner_id")
            connection.execSQL(
                """
                UPDATE chat_channels
                SET members = REPLACE(members, '"id":', '"peerId":')
                WHERE members LIKE '%"id"%'
                """.trimIndent()
            )
            connection.execSQL("ALTER TABLE video_play_progress RENAME COLUMN duration TO position_ms")
            connection.execSQL("ALTER TABLE audio_queue_items RENAME COLUMN duration TO duration_ms")
            connection.execSQL("ALTER TABLE audio_playlist_items RENAME COLUMN duration TO duration_ms")
            connection.execSQL("ALTER TABLE audio_play_history RENAME COLUMN duration TO duration_ms")
            connection.execSQL("ALTER TABLE media_item RENAME COLUMN duration TO duration_ms")
            connection.execSQL("UPDATE audio_queue_items SET duration_ms = duration_ms * 1000")
            connection.execSQL("UPDATE audio_playlist_items SET duration_ms = duration_ms * 1000")
            connection.execSQL("UPDATE audio_play_history SET duration_ms = duration_ms * 1000")
            connection.execSQL("UPDATE media_item SET duration_ms = duration_ms * 1000")
            connection.execSQL("ALTER TABLE clipboard RENAME TO clipboards")
            // archived_conversations.conversation_date: INTEGER epoch millis → TEXT ISO-8601.
            connection.execSQL(
                """
                CREATE TABLE archived_conversations_new (
                    conversation_id TEXT NOT NULL,
                    conversation_date TEXT NOT NULL,
                    PRIMARY KEY(conversation_id)
                )
                """.trimIndent()
            )
            connection.execSQL(
                "INSERT INTO archived_conversations_new (conversation_id, conversation_date) " +
                    "SELECT conversation_id, conversation_date FROM archived_conversations"
            )
            val rows = mutableListOf<Pair<String, Long>>()
            connection.prepare("SELECT conversation_id, conversation_date FROM archived_conversations_new").use { stmt ->
                while (stmt.step()) {
                    rows.add(Pair(stmt.getText(0), stmt.getLong(1)))
                }
            }
            connection.prepare("UPDATE archived_conversations_new SET conversation_date = ? WHERE conversation_id = ?").use { update ->
                rows.forEach { (id, epochMillis) ->
                    update.bindText(1, kotlin.time.Instant.fromEpochMilliseconds(epochMillis).toString())
                    update.bindText(2, id)
                    update.step()
                    update.reset()
                }
            }
            connection.execSQL("DROP TABLE archived_conversations")
            connection.execSQL("ALTER TABLE archived_conversations_new RENAME TO archived_conversations")
        }
    }

    /**
     * All manual migrations in the order they should be applied.
     * Register this array with `addMigrations()` on the platform-specific builder.
     */
    val ALL = arrayOf(
        MIGRATION_5_6,
        MIGRATION_18_19,
        MIGRATION_19_20,
        MIGRATION_20_21,
        MIGRATION_21_22,
        MIGRATION_22_23,
        MIGRATION_30_31,
    )
}
