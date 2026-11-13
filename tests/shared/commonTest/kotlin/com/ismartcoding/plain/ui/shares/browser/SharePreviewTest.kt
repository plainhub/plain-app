package com.ismartcoding.plain.ui.shares.browser

import com.ismartcoding.plain.features.files.FilesApi
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.File
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import com.ismartcoding.plain.rust.graphql.GraphqlResponse
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class SharePreviewTest {
    private val entry = File(name = "note.txt", path = "https://device.test/s/share/note.txt#key", url = "https://device.test/fs?id=s.encrypted&action=download", createdAt = null, updatedAt = Instant.fromEpochMilliseconds(1234), size = 8, isDir = false, childCount = 0)

    @Test
    fun original_and_versioned_thumbnail_use_generic_copy_with_current_cache_destination() = runTest {
        val sources = mutableListOf<String>()
        val files = FilesApi(GraphqlClient { operation ->
            assertEquals(JsonHelper.jsonEncodeToElement("/owned/cache"), operation.variables["directory"])
            sources += operation.variables["src"].toString().trim('"')
            GraphqlResponse(data = JsonHelper.jsonDecode("""{"copyFileToDirectory":"/owned/cache/output.txt"}"""))
        }, upload = { _, _ -> error("unexpected upload") })
        assertEquals("/owned/cache/output.txt", SharePreview.copy(entry, directory = "/owned/cache", files = files))
        SharePreview.copy(entry, thumbnail = true, directory = "/owned/cache", files = files)
        assertEquals(listOf(entry.url, "https://device.test/fs?id=s.encrypted&action=preview&size=sm&v=1234"), sources)
    }

    @Test
    fun unsuccessful_copy_does_not_open_a_remote_path() = runTest {
        val files = FilesApi(GraphqlClient { GraphqlResponse(data = JsonHelper.jsonDecode("""{"copyFileToDirectory":""}""")) }, upload = { _, _ -> error("unexpected upload") })
        assertFailsWith<IllegalStateException> { SharePreview.copy(entry, directory = "/owned/cache", files = files) }
    }
}
