package com.ismartcoding.plain.db

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Source-contract lock for MIGRATION_30_31 (the host test environment cannot
 * load the bundled SQLite native driver, so the migration SQL is asserted
 * verbatim against the migration source — same style as DialogUniformityGuardTest).
 *
 * Locks:
 * - every renamed column has a RENAME COLUMN in the migration;
 * - the seconds→ms conversions run for all four duration tables;
 * - the members-JSON key rewrite and the Instant conversion stay present;
 * - `feeds` must NOT be touched (entryCount is an @Ignore field — no column);
 * - the v31 schema JSON carries the new column names.
 */
class Migration30to31Test {

    private val migrationSource: String by lazy {
        java.io.File("../room-db/src/commonMain/kotlin/com/ismartcoding/plain/db/Migrations.kt")
            .readText()
            .substringAfter("MIGRATION_30_31")
            .substringBefore("val ALL")
    }

    private val schema31: String by lazy {
        java.io.File("../room-db/schemas/com.ismartcoding.plain.platform.AppDatabase/31.json").readText()
    }

    @Test
    fun migrationRenamesEveryColumnTouchedByTheEntities() {
        val renames = listOf(
            "chat_channels" to "owner_id",
            "video_play_progress" to "position_ms",
            "audio_queue_items" to "duration_ms",
            "audio_playlist_items" to "duration_ms",
            "audio_play_history" to "duration_ms",
            "media_item" to "duration_ms",
        )
        renames.forEach { (table, column) ->
            val pattern = "ALTER TABLE $table RENAME COLUMN"
            assertTrue(migrationSource.contains(pattern), "migration must rename a column on $table (expected $column)")
        }
        assertTrue(migrationSource.contains("ALTER TABLE clipboard RENAME TO clipboards"), "clipboard table must be renamed to clipboards")
    }

    @Test
    fun migrationConvertsSecondsToMillisForAllDurationTables() {
        listOf("audio_queue_items", "audio_playlist_items", "audio_play_history", "media_item").forEach { table ->
            assertTrue(
                migrationSource.contains("UPDATE $table SET duration_ms = duration_ms * 1000"),
                "$table seconds→ms conversion is missing",
            )
        }
    }

    @Test
    fun migrationRewritesMembersJsonAndArchivedDates() {
        assertTrue(
            migrationSource.contains("""REPLACE(members, '"id":', '"peerId":')"""),
            "members JSON key rewrite id→peerId is missing",
        )
        assertTrue(
            migrationSource.contains("Instant.fromEpochMilliseconds"),
            "archived_conversations epoch→Instant conversion is missing",
        )
    }

    @Test
    fun migrationMustNotTouchFeedsTable() {
        // entryCount is an @Ignore in-memory field — the feeds table has no count column.
        assertTrue(!migrationSource.contains("feeds"), "feeds has no persisted count column; a feeds migration would crash")
    }

    @Test
    fun schema31CarriesTheNewColumnNames() {
        val expectedColumns = mapOf(
            "chat_channels" to listOf("owner_id"),
            "video_play_progress" to listOf("position_ms"),
            "audio_queue_items" to listOf("duration_ms"),
            "audio_playlist_items" to listOf("duration_ms"),
            "audio_play_history" to listOf("duration_ms"),
            "media_item" to listOf("duration_ms"),
        )
        expectedColumns.forEach { (table, columns) ->
            columns.forEach { column ->
                assertTrue(schema31.contains("\"columnName\": \"$column\""), "schema v31 must expose $table.$column")
            }
        }
        assertTrue(schema31.contains("\"tableName\": \"clipboards\""), "schema v31 must name the table clipboards")
        assertTrue(!schema31.contains("\"tableName\": \"clipboard\""), "old clipboard table must be gone")
    }
}
