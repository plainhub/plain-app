package com.ismartcoding.plain.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create new table with desired structure
            db.execSQL("""
                CREATE TABLE chats_new (
                    id TEXT PRIMARY KEY NOT NULL,
                    from_id TEXT NOT NULL,
                    to_id TEXT NOT NULL,
                    group_id TEXT NOT NULL,
                    content TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL
                )
            """)
            
            // Copy and transform data
            db.execSQL("""
                INSERT INTO chats_new (id, from_id, to_id, group_id, content, created_at, updated_at)
                SELECT id, 
                       CASE WHEN is_me = 1 THEN 'me' ELSE 'local' END as from_id,
                       CASE WHEN is_me = 1 THEN 'local' ELSE 'me' END as to_id,
                       '',
                       content, created_at, updated_at 
                FROM chats
            """)
            
            // Create new tables
            db.execSQL("""
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
            
            db.execSQL("""
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
            db.execSQL("DROP TABLE chats")
            db.execSQL("ALTER TABLE chats_new RENAME TO chats")
        }
    }
} 