package com.ismartcoding.plain.features.audio

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import com.ismartcoding.plain.rust.graphql.GraphqlResponse
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AudioQueueClientTest {
    private fun client(response: String) = AudioQueueApi(GraphqlClient {
        GraphqlResponse(data = JsonHelper.jsonDecode(response))
    })

    @Test
    fun absent_target_speed_uses_its_default_and_saved_speeds_remain_device_scoped() = runTest {
        val unset = client("""{"prefs":{}}""")
        val first = client("""{"prefs":{"audio_playback_speed":1.5}}""")
        val second = client("""{"prefs":{"audio_playback_speed":2.0}}""")
        assertEquals(1f, unset.playbackSpeed())
        assertEquals(1.5f, first.playbackSpeed())
        assertEquals(2f, second.playbackSpeed())
        assertEquals(1f, unset.playbackSpeed())
    }

    @Test
    fun reads_remote_playback_and_speed_in_one_request() = runTest {
        var requests = 0
        val api = AudioQueueApi(GraphqlClient {
            requests++
            GraphqlResponse(data = JsonHelper.jsonDecode("""{"audioPlayback":{"mediaId":"target-track","path":"/target.mp3","mode":"REPEAT","isPlaying":false,"positionMs":17,"revision":3},"prefs":{"audio_playback_speed":1.5}}"""))
        })
        val settings = api.playbackSettings()
        assertEquals("target-track", settings.audioPlayback.mediaId)
        assertEquals(3L, settings.audioPlayback.revision)
        assertEquals(1.5f, settings.speed)
        assertEquals(1, requests)
    }

    @Test
    fun combined_playback_settings_keep_defaults_local_to_the_reader_and_reject_invalid_values() = runTest {
        val base = """"audioPlayback":{"mediaId":null,"path":null,"mode":"REPEAT","isPlaying":false,"positionMs":0,"revision":0}"""
        val unset = client("{$base,\"prefs\":{}}")
        assertEquals(1f, unset.playbackSettings().speed)
        val invalid = client("{$base,\"prefs\":{\"audio_playback_speed\":\"invalid\"}}")
        assertFailsWith<IllegalArgumentException> { invalid.playbackSettings().speed }
    }

    @Test
    fun queue_snapshot_reads_state_window_and_membership_in_one_request() = runTest {
        var requests = 0
        val api = AudioQueueApi(GraphqlClient {
            requests++
            GraphqlResponse(data = JsonHelper.jsonDecode("""{"audioPlayback":{"mediaId":"target-track","path":"/target.mp3","mode":"REPEAT","isPlaying":false,"positionMs":0,"revision":3},"prefs":{},"audioQueueItemCount":2,"audioQueueItems":[{"mediaId":"target-track","path":"/target.mp3","title":"Track","artist":"","durationMs":1000}],"count":2,"items":[{"mediaId":"target-track","path":"/target.mp3","title":"Track","artist":"","durationMs":1000}],"queuedIds":[{"mediaId":"target-track"},{"mediaId":"other-track"}]}"""))
        })
        val snapshot = api.snapshot(1)
        assertEquals(2, snapshot.count)
        assertEquals("target-track", snapshot.items.single().mediaId)
        assertEquals("target-track", snapshot.audioPlayback.mediaId)
        assertEquals(1, requests)
    }

    @Test
    fun remote_enqueue_then_select_uses_one_ordered_mutation_and_in_place_play_does_not_enqueue() = runTest {
        val operations = mutableListOf<com.ismartcoding.plain.rust.graphql.GraphqlOperation>()
        val api = AudioQueueApi(GraphqlClient { operation ->
            operations.add(operation)
            GraphqlResponse(data = JsonHelper.jsonDecode("""{"addAudioToQueue":true,"playAudio":{"mediaId":"target-track","path":"/target.mp3","title":"Track","artist":"","durationMs":1000}}"""))
        })
        assertEquals("target-track", api.select("target-track", enqueue = true).mediaId)
        assertEquals(1, operations.size)
        kotlin.test.assertTrue(operations.single().query.indexOf("addAudioToQueue(") < operations.single().query.indexOf("playAudio("))
        operations.clear()
        api.select("target-track", enqueue = false)
        assertEquals(1, operations.size)
        kotlin.test.assertFalse(operations.single().query.contains("addAudioToQueue"))
    }

    @Test
    fun invalid_saved_speed_is_not_replaced_with_a_default() = runTest {
        val target = client("""{"prefs":{"audio_playback_speed":"invalid"}}""")
        assertFailsWith<IllegalArgumentException> { target.playbackSpeed() }
    }
}
