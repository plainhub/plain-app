package com.ismartcoding.plain.db

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Locks the manual-ordering column convention (2026-09-21) against the Room
 * exported schema history: every column storing a manual order index MUST be
 * named `sort_order`.
 *
 * History: bookmarks/bookmark_groups were born as `sort_order`; auto-migration
 * 29→30 renamed audio_playlist_items.position, audio_queue_items.position and
 * book_chapters.display_order. `sort_by` is exempt on purpose — it stores a
 * sort criterion name ("TITLE"/"DATE"...), not an order index. Same for the
 * audio_queue_source.current_index cursor.
 *
 * Source of truth is the Room-exported schema JSON, so any entity added to
 * @Database is covered automatically.
 */
class SortOrderNamingGuardTest {

    private val schemaDirName = "room-db/schemas/com.ismartcoding.plain.platform.AppDatabase"

    /** Resolve the room-db schema dir from whatever the test working dir is. */
    private fun schemaFile(version: Int): File {
        var dir = File(System.getProperty("user.dir")).absoluteFile
        repeat(4) {
            val f = File(File(dir, schemaDirName), "$version.json")
            if (f.isFile) return f
            dir = dir.parentFile ?: return@repeat
        }
        fail("schema $version.json not found under any $schemaDirName (cwd=${System.getProperty("user.dir")})")
    }

    private fun schema(version: Int): JsonObject =
        Json.parseToJsonElement(schemaFile(version).readText()).jsonObject

    private fun JsonObject.entities(): List<JsonObject> =
        this["database"]!!.jsonObject["entities"]!!.jsonArray.map { it.jsonObject }

    private fun JsonObject.tableName() = this["tableName"]!!.jsonPrimitive.content
    private fun JsonObject.columns() =
        this["fields"]!!.jsonArray.map { it.jsonObject["columnName"]!!.jsonPrimitive.content }
    private fun JsonObject.indexNames() =
        this["indices"]!!.jsonArray.map { it.jsonObject["name"]!!.jsonPrimitive.content }

    private fun JsonObject.table(name: String): JsonObject =
        assertNotNull(
            entities().firstOrNull { it.tableName() == name },
            "table $name missing from @Database",
        )

    /** Tables whose manual order index must be exactly `sort_order`. */
    private val sortOrderTables = listOf(
        "bookmarks", "bookmark_groups",
        "audio_playlist_items", "audio_queue_items", "book_chapters",
    )

    /** Legacy order-index column names that must never come back. */
    private val forbiddenColumns = setOf(
        "position", "display_order", "displayOrder",
        "sort_index", "sortIndex", "order_index", "orderIndex",
        "sort_no", "seq", "order",
    )

    @Test
    fun schemaDiscoveryIsNotVacuous() {
        val db = schema(30)
        val entities = db.entities()
        assertTrue(entities.size >= 25, "suspiciously few entities (${entities.size}) — schema parse broken")
        assertEquals(30, db["database"]!!.jsonObject["version"]!!.jsonPrimitive.content.toInt())
        assertEquals(sortOrderTables.size, 5)
    }

    @Test
    fun manualOrderColumnsAreNamedSortOrder() {
        val current = schema(30)
        val missing = sortOrderTables.filter { "sort_order" !in current.table(it).columns() }
        if (missing.isNotEmpty()) fail("tables missing `sort_order` column: $missing")
    }

    @Test
    fun noLegacyOrderingColumnNames() {
        val offenders = schema(30).entities()
            .flatMap { e -> e.columns().filter { it in forbiddenColumns }.map { "${e.tableName()}.$it" } }
        if (offenders.isNotEmpty()) fail("legacy ordering columns found (use sort_order): $offenders")
    }

    /** Locks the 29→30 rename story: what the migration left and what it produced. */
    @Test
    fun migrationRenamedPositionAndDisplayOrderToSortOrder() {
        val v29 = schema(29)
        assertEquals(listOf("position"), v29.table("audio_playlist_items").columns().filter { it == "position" })
        assertEquals(listOf("position"), v29.table("audio_queue_items").columns().filter { it == "position" })
        assertEquals(listOf("display_order"), v29.table("book_chapters").columns().filter { it == "display_order" })
        assertTrue(
            "index_audio_playlist_items_playlist_id_position" in v29.table("audio_playlist_items").indexNames(),
            "v29 index name drifted — update this test",
        )

        val v30 = schema(30)
        val plIdx = v30.table("audio_playlist_items").indexNames()
        val qIdx = v30.table("audio_queue_items").indexNames()
        assertTrue("index_audio_playlist_items_playlist_id_sort_order" in plIdx, "v30 playlist index: $plIdx")
        assertTrue("index_audio_queue_items_sort_order" in qIdx, "v30 queue index: $qIdx")
        assertTrue(
            "index_audio_playlist_items_playlist_id_position" !in plIdx &&
                "index_audio_queue_items_position" !in qIdx,
            "legacy index names must not survive the rename",
        )
    }
}
