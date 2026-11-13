package com.ismartcoding.plain.features.audio

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import com.ismartcoding.plain.rust.graphql.GraphqlOperation
import com.ismartcoding.plain.rust.graphql.GraphqlResponse
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AudioHomeContentTest {
    private fun audio(id: String) = """{"id":"$id","title":"Song","artist":"Artist","path":"/music/song.mp3","durationMs":1234,"size":2000,"bucketId":"music","albumId":"album","albumFileId":"cover","createdAt":"2026-01-01T00:00:00Z","updatedAt":"2026-01-01T00:00:00Z","isFavorite":true}"""

    @Test
    fun artists_and_recent_audio_share_one_request_and_preserve_history_order() = runTest {
        val requests = mutableListOf<GraphqlOperation>()
        val api = AudioOverviewApi(GraphqlClient { operation ->
            requests += operation
            val history = listOf("second", "first").joinToString(",") {
                """{"media":{"__typename":"Audio",${audio(it).drop(1)}}"""
            }
            GraphqlResponse(data = JsonHelper.jsonDecode("""{
                "audioArtists":[{"name":"Artist","itemCount":1205,"playCount":3000000123,"samplePath":"/music/song.mp3"}],
                "mediaPlayHistory":[$history]
            }"""))
        })
        val content = api.read()
        assertEquals(1205, content.artists.single().itemCount)
        assertEquals(3000000123L, content.artists.single().playCount)
        assertEquals(listOf("second", "first"), content.recentItems.map { it.id })
        assertEquals("album", content.recentItems.first().albumId)
        assertTrue(content.recentItems.first().isFavorite)
        val query = requests.single().query
        assertTrue("audioArtists {" in query)
        assertTrue("""mediaPlayHistory(query: "type:AUDIO", offset: 0, limit: 8)""" in query)
    }

    @Test
    fun empty_history_loads_at_most_eight_most_recent_audio_files() = runTest {
        val requests = mutableListOf<GraphqlOperation>()
        val api = AudioOverviewApi(GraphqlClient { operation ->
            requests += operation
            val response = if (requests.size == 1) """{"audioArtists":[],"mediaPlayHistory":[]}"""
            else """{"audios":[${audio("newest")}]}"""
            GraphqlResponse(data = JsonHelper.jsonDecode(response))
        })
        assertEquals("newest", api.read().recentItems.single().id)
        assertEquals(2, requests.size)
        assertEquals(
            mapOf("query" to JsonPrimitive(""), "offset" to JsonPrimitive(0), "limit" to JsonPrimitive(8), "sortBy" to JsonPrimitive("DATE_DESC")),
            requests.last().variables,
        )
    }
}
