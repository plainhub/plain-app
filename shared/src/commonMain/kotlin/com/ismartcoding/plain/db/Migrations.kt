package com.ismartcoding.plain.db

import androidx.room.migration.Migration
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
        override fun migrate(connection: SQLiteConnection) {
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
                    status TEXT NOT NULL,
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
}
