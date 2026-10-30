package com.ismartcoding.plain.features.media

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.SelectedMediaPath
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import com.ismartcoding.plain.rust.graphql.GraphqlResponse
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class MediaCatalogClientTest {
    @Test
    fun remote_share_resolves_selected_device_paths_in_requested_order() = runTest {
        val resolved = mutableListOf<String>()
        val client = MediaCatalogApi(GraphqlClient {
            GraphqlResponse(data = JsonObject(mapOf("images" to JsonHelper.jsonEncodeToElement(listOf(
                SelectedMediaPath("second", "/remote/second.png"),
                SelectedMediaPath("first", "/remote/first.png"),
            )))))
        }) { type, id, path ->
            assertEquals(DataType.IMAGE, type)
            resolved.add("$id:$path")
            "/cache/$id.png"
        }
        assertEquals(listOf("/cache/first.png", "/cache/second.png"), client.shareUris(DataType.IMAGE, listOf("first", "second")))
        assertEquals(listOf("first:/remote/first.png", "second:/remote/second.png"), resolved)
    }

    @Test
    fun artist_tracks_use_exact_filter_and_read_past_the_first_page() = runTest {
        val artist = "> Duo: \"Live\" \\ Set"
        val requests = mutableListOf<com.ismartcoding.plain.rust.graphql.GraphqlOperation>()
        val client = MediaCatalogApi(GraphqlClient { operation ->
            requests.add(operation)
            val offset = operation.variables.getValue("offset").toString().toInt()
            val end = minOf(offset + 500, 501)
            val tracks = (offset until end).map { id ->
                com.ismartcoding.plain.models.Audio(
                    id = "$id", title = "Track $id", artist = artist, path = "/audio/$id.mp3",
                    durationMs = 1000, size = 100, bucketId = "audio",
                    createdAt = kotlin.time.Instant.fromEpochMilliseconds(0),
                    updatedAt = kotlin.time.Instant.fromEpochMilliseconds(0),
                )
            }
            GraphqlResponse(data = JsonObject(mapOf("audios" to JsonHelper.jsonEncodeToElement(tracks))))
        })
        val tracks = client.artistTracks(artist, com.ismartcoding.plain.features.files.FileSortBy.NAME_ASC)
        assertEquals(501, tracks.size)
        assertEquals("500", tracks.last().id)
        assertEquals(listOf("0", "500"), requests.map { it.variables.getValue("offset").toString() })
        val query = JsonHelper.jsonDecodeFromElement<String>(requests.first().variables.getValue("query"))
        val field = com.ismartcoding.plain.helpers.SearchHelper.parse(query).single()
        assertEquals("artist", field.name)
        assertEquals("=", field.op)
        assertEquals(artist, field.value)
    }
}
