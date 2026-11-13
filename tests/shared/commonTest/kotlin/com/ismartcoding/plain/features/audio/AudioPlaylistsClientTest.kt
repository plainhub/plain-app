package com.ismartcoding.plain.features.audio

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import com.ismartcoding.plain.rust.graphql.GraphqlOperation
import com.ismartcoding.plain.rust.graphql.GraphqlResponse
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AudioPlaylistsClientTest {
    private val playlist = """{"id":"playlist","name":"Playlist","itemCount":5,"createdAt":"2026-01-01T00:00:00Z","updatedAt":"2026-01-01T00:00:00Z","coverAlbums":[{"albumId":"album","trackCount":5,"candidatePaths":["/one.mp3","/two.mp3","/three.mp3"]}]}"""

    @Test
    fun list_loads_metadata_and_cover_candidates_in_one_request() = runTest {
        val requests = mutableListOf<GraphqlOperation>()
        val api = AudioPlaylistsApi(GraphqlClient { operation ->
            requests += operation
            GraphqlResponse(data = JsonHelper.jsonDecode("""{"audioPlaylists":[$playlist]}"""))
        })
        val (row, count) = api.playlists(includeCovers = true).single()
        assertEquals(5, count)
        assertEquals("album", row.coverAlbums.single().albumId)
        assertEquals(listOf("/one.mp3", "/two.mp3", "/three.mp3"), row.coverAlbums.single().candidatePaths)
        assertTrue("coverAlbums { albumId trackCount candidatePaths }" in requests.single().query)
        assertFalse("audioPlaylistCovers" in requests.single().query)
    }

    @Test
    fun ordinary_lists_do_not_request_cover_computation() = runTest {
        val requests = mutableListOf<GraphqlOperation>()
        val api = AudioPlaylistsApi(GraphqlClient { operation ->
            requests += operation
            GraphqlResponse(data = JsonHelper.jsonDecode("""{"audioPlaylists":[]}"""))
        })
        assertTrue(api.playlists().isEmpty())
        assertFalse("coverAlbums" in requests.single().query)
    }

    @Test
    fun detail_requests_only_one_playlist_with_its_cover_candidates() = runTest {
        val requests = mutableListOf<GraphqlOperation>()
        val api = AudioPlaylistsApi(GraphqlClient { operation ->
            requests += operation
            GraphqlResponse(data = JsonHelper.jsonDecode("""{"audioPlaylist":$playlist}"""))
        })
        assertEquals("album", api.playlist("playlist")?.coverAlbums?.single()?.albumId)
        val request = requests.single()
        assertEquals(mapOf("id" to JsonPrimitive("playlist")), request.variables)
        assertTrue("audioPlaylist(id:" in request.query)
        assertTrue("coverAlbums" in request.query)
        assertFalse("audioPlaylists {" in request.query)
    }

    @Test
    fun missing_playlist_returns_null() = runTest {
        val api = AudioPlaylistsApi(GraphqlClient {
            GraphqlResponse(data = JsonHelper.jsonDecode("""{"audioPlaylist":null}"""))
        })
        assertNull(api.playlist("missing"))
    }
}
