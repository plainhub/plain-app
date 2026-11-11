package com.ismartcoding.plain.tests

import android.os.Environment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.features.web.AccessFeature
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.rust.graphql.GraphqlRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageSearchPaginationDeviceTest {
    private suspend fun execute(document: String, variables: Map<String, JsonElement> = emptyMap()) =
        GraphqlApi.execute(GraphqlRequest<JsonObject>(document, variables))

    private suspend fun status() = execute("query { imageSearchStatus { status isIndexing indexedImages } }")
        .getValue("imageSearchStatus").jsonObject

    private suspend fun page(query: String, offset: Int, limit: Int) = execute(
        "query(\$query: String!, \$offset: Int!, \$limit: Int!) { images(query: \$query, offset: \$offset, limit: \$limit, sortBy: NAME_ASC) { id bucketId } }",
        mapOf("query" to JsonPrimitive(query), "offset" to JsonPrimitive(offset), "limit" to JsonPrimitive(limit)),
    ).getValue("images").jsonArray.map { it.jsonObject }

    private suspend fun count(query: String) = execute(
        "query(\$query: String!) { imageCount(query: \$query) }",
        mapOf("query" to JsonPrimitive(query)),
    ).getValue("imageCount").jsonPrimitive.int

    private suspend fun enumerate(query: String): List<JsonObject> {
        val expected = count(query)
        val result = mutableListOf<JsonObject>()
        val seen = mutableSetOf<String>()
        var offset = 0
        while (offset < expected) {
            val rows = page(query, offset, 37)
            assertEquals("Page length", minOf(37, expected - offset), rows.size)
            rows.forEach { assertTrue("Duplicate image across pages", seen.add(it.getValue("id").jsonPrimitive.content)) }
            result.addAll(rows)
            offset += rows.size
        }
        assertEquals("Count changed while paging", expected, count(query))
        assertTrue("Page after total must be empty", page(query, expected, 37).isEmpty())
        assertTrue("Zero limit must be empty", page(query, 0, 0).isEmpty())
        assertTrue("Page replay must be stable", result.take(37) == page(query, 0, 37))
        return result
    }

    private suspend fun awaitIndex(expected: Int? = null) {
        withTimeout(600_000) {
                var reported = -1
                var idleSamples = 0
                while (true) {
                    val value = status()
                    val indexedNow = value.getValue("indexedImages").jsonPrimitive.int
                    if (indexedNow != reported) {
                        InstrumentationRegistry.getInstrumentation().sendStatus(0, android.os.Bundle().apply { putInt("indexedImages", indexedNow) })
                        reported = indexedNow
                    }
                    check(value.getValue("status").jsonPrimitive.content != "ERROR") { "Image model failed" }
                    if (value.getValue("status").jsonPrimitive.content == "READY" && !value.getValue("isIndexing").jsonPrimitive.boolean && (expected == null || indexedNow == expected)) idleSamples++ else idleSamples = 0
                    if (idleSamples >= 6) break
                    delay(500)
                }
            }
    }

    @Test
    fun semanticPagesCountsAndFoldersUseNormalGraphql() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        check(args.getString("allowImageSearchAcceptance") == "true") { "Explicit local image-search acceptance required" }
        check(appContext.packageName == "com.ismartcoding.plain.acceptance") { "Independent acceptance package required" }
        check(Environment.isExternalStorageManager()) { "Existing system image-library permission required" }
        val original = Prefs.apiAccessFeatures.value
        val generated = mutableListOf<android.net.Uri>()
        var baselineIndexed: Int? = null
        val prefix = "plainsemantic" + java.util.UUID.randomUUID().toString().replace("-", "")
        try {
            Prefs.setApiAccessFeatures(original + AccessFeature.IMAGES)
            if (args.getString("importImageSearchFixture") == "true") {
                check(!java.io.File(appContext.filesDir, "ai_models/imported").exists() && !java.io.File(appContext.filesDir, "ai_models/default").exists()) { "Refusing to replace model files" }
                check(status().getValue("status").jsonPrimitive.content == "UNAVAILABLE") { "Refusing to replace an existing image model" }
                execute("mutation { importImageSearchModel }")
            }
            awaitIndex()
            val manifestFile = java.io.File(appContext.filesDir, "ai_models/imported/manifest.json")
            val manifest = Json.parseToJsonElement(manifestFile.readText()).jsonObject
            check(manifest.getValue("id").jsonPrimitive.content == "fixture" && manifest.getValue("minimumScore").jsonPrimitive.double == -1.0) { "All-match offline fixture required" }
            for (file in manifest.getValue("files").jsonArray) {
                val item = file.jsonObject
                val bytes = java.io.File(manifestFile.parentFile, item.getValue("name").jsonPrimitive.content).readBytes()
                val hash = java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
                check(hash == item.getValue("sha256").jsonPrimitive.content) { "Fixture checksum mismatch" }
            }
            if (args.getString("addSyntheticImages") == "true") {
                val resolver = appContext.contentResolver
                baselineIndexed = status().getValue("indexedImages").jsonPrimitive.int
                val required = (620 - baselineIndexed).coerceAtLeast(3)
                repeat(required) { number ->
                    val values = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "$prefix-$number.png")
                        put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PlainSearchAcceptance/$prefix")
                        put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
                    }
                    val uri = checkNotNull(resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
                    generated.add(uri)
                    val bitmap = android.graphics.Bitmap.createBitmap(256, 256, android.graphics.Bitmap.Config.ARGB_8888)
                    try {
                        bitmap.eraseColor(0xff336699.toInt())
                        checkNotNull(resolver.openOutputStream(uri)).use { check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)) }
                    } finally { bitmap.recycle() }
                    values.clear()
                    values.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
                    assertEquals(1, resolver.update(uri, values, null, null))
                }
                delay(3_000)
                execute("mutation { startImageIndex }")
                awaitIndex(baselineIndexed + generated.size)
            }
            val text = "paginationprobe" + java.util.UUID.randomUUID().toString().replace("-", "")
            val filenameMatches = appContext.contentResolver.query(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf(android.provider.MediaStore.Images.Media._ID), "_display_name LIKE ?", arrayOf("%$text%"), null)!!.use { it.count }
            assertEquals("Unique query must not match filenames", 0, filenameMatches)
            val indexed = status().getValue("indexedImages").jsonPrimitive.int
            val query = "text:${JsonPrimitive(text)}"
            val all = enumerate(query)
            assertTrue("Need more than 50 indexed images to exercise regression", indexed > 50)
            assertEquals("All-match fixture must return every successfully indexed image", indexed, all.size)
            if (generated.isNotEmpty()) {
                assertTrue("Synthetic fixture must cover beyond 500", indexed > 500)
                val overlap = enumerate("text:${JsonPrimitive(prefix)}")
                assertEquals("Filename and semantic union must deduplicate", indexed, overlap.size)
                assertTrue("Filename matches must appear first", overlap.take(generated.size).map { it.getValue("id").jsonPrimitive.content }.toSet() == generated.map { checkNotNull(it.lastPathSegment) }.toSet())
            }
            val bucket = all.groupBy { it.getValue("bucketId").jsonPrimitive.content }.maxBy { it.value.size }
            val folderQuery = "$query bucket_id:${JsonPrimitive(bucket.key)}"
            val folder = enumerate(folderQuery)
            assertTrue("Folder membership must match global search", bucket.value.map { it.getValue("id") }.toSet() == folder.map { it.getValue("id") }.toSet())
            assertTrue("Folder results must stay inside folder", folder.all { it.getValue("bucketId").jsonPrimitive.content == bucket.key })
            val beyond = all.drop(500).firstOrNull() ?: all.drop(50).first()
            val lateBucket = beyond.getValue("bucketId").jsonPrimitive.content
            val lateFolder = enumerate("$query bucket_id:${JsonPrimitive(lateBucket)}")
            assertTrue("Folder must retain matches beyond the old candidate cap", lateFolder.any { it.getValue("id") == beyond.getValue("id") })
            val report = android.os.Bundle().apply {
                putInt("matchedImages", all.size)
                putInt("indexedImages", indexed)
                putInt("folderMatches", folder.size)
                putBoolean("beyond500", all.size > 500)
                putInt("syntheticImages", generated.size)
            }
            InstrumentationRegistry.getInstrumentation().sendStatus(0, report)
        } finally {
            try {
            generated.forEachIndexed { number, uri ->
                val resolver = appContext.contentResolver
                val matches = resolver.query(uri, arrayOf(android.provider.MediaStore.Images.Media.DISPLAY_NAME, android.provider.MediaStore.Images.Media.RELATIVE_PATH), null, null, null)?.use { row ->
                    row.moveToFirst() && row.getString(0) == "$prefix-$number.png" && row.getString(1).trimEnd('/') == "Pictures/PlainSearchAcceptance/$prefix"
                } == true
                check(matches) { "Synthetic image identity changed; preserving it" }
                assertEquals("Synthetic image cleanup", 1, resolver.delete(uri, null, null))
            }
            if (generated.isNotEmpty()) {
                delay(3_000)
                execute("mutation { startImageIndex }")
                awaitIndex(baselineIndexed)
                InstrumentationRegistry.getInstrumentation().sendStatus(0, android.os.Bundle().apply { putInt("restoredIndexedImages", checkNotNull(baselineIndexed)) })
            }
            } finally { Prefs.setApiAccessFeatures(original) }
        }
    }
}
