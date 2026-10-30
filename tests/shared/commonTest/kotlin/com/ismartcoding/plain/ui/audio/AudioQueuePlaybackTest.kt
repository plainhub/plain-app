package com.ismartcoding.plain.ui.audio

import androidx.lifecycle.ViewModelStore
import com.ismartcoding.plain.features.audio.AudioQueueApi
import com.ismartcoding.plain.features.files.FileSortBy
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.AudioItem
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import com.ismartcoding.plain.rust.graphql.GraphqlResponse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class AudioQueuePlaybackTest {
    @Test
    fun dismissing_or_denying_permission_keeps_the_current_queue_and_track() = withPlayer { player ->
        player.vm.playQuery("artist:Artist", FileSortBy.NAME_DESC)
        runCurrent()
        assertNotNull(player.onGranted)
        assertEquals(listOf("permission"), player.calls)
        assertEquals("old", player.vm.selectedMediaId.value)
        assertEquals("/old.mp3", player.vm.selectedPath.value)
        assertEquals(listOf("old"), player.vm.queueItems.value.map { it.mediaId })
    }

    @Test
    fun permission_must_finish_before_replacing_and_starting_playback() = withPlayer { player ->
        player.vm.playQuery("artist:Artist", FileSortBy.NAME_DESC)
        runCurrent()
        assertEquals(listOf("permission"), player.calls)
        player.onGranted!!.invoke()
        player.refreshed.await()
        runCurrent()
        assertEquals(listOf("permission", "replace", "play", "refresh"), player.calls)
        assertEquals("new", player.vm.selectedMediaId.value)
        assertEquals("/new.mp3", player.vm.selectedPath.value)
    }

    @Test
    fun granting_permission_after_the_view_model_is_cleared_does_not_change_playback() = withPlayer { player ->
        player.vm.playQuery("artist:Artist", FileSortBy.NAME_DESC)
        player.store.clear()
        player.onGranted!!.invoke()
        runCurrent()
        assertEquals(listOf("permission"), player.calls)
        assertEquals("old", player.vm.selectedMediaId.value)
    }

    private fun withPlayer(test: suspend TestScope.(Player) -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val player = Player()
        try {
            test(player)
        } finally {
            player.store.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    private class Player {
        val calls = mutableListOf<String>()
        val refreshed = CompletableDeferred<Unit>()
        var onGranted: (() -> Unit)? = null
        private val queue = AudioQueueApi(GraphqlClient { request ->
            if (request.query.contains("replaceAudioQueue")) {
                calls.add("replace")
                assertEquals("\"NAME_DESC\"", request.variables.getValue("sortBy").toString())
                GraphqlResponse(data = JsonHelper.jsonDecode("""{"replaceAudioQueue":{"mediaId":"new","title":"New","artist":"Artist","path":"/new.mp3","durationMs":1000}}"""))
            } else {
                calls.add("refresh")
                refreshed.complete(Unit)
                GraphqlResponse(data = JsonHelper.jsonDecode("""{"audioPlayback":{"mediaId":"new","path":"/new.mp3","mode":"REPEAT","isPlaying":true,"positionMs":0,"revision":2},"prefs":{},"count":1,"items":[{"mediaId":"new","title":"New","artist":"Artist","path":"/new.mp3","durationMs":1000}],"queuedIds":[{"mediaId":"new"}]}"""))
            }
        })
        val vm = AudioQueueViewModel(
            queue = queue,
            checkPlaybackPermission = { granted -> calls.add("permission"); onGranted = granted },
            startPlayback = { calls.add("play") },
            sourceChanges = emptyFlow(),
        ).apply {
            selectedMediaId.value = "old"
            selectedPath.value = "/old.mp3"
            queueItems.value = listOf(AudioItem(mediaId = "old", title = "Old", artist = "Artist", path = "/old.mp3", durationMs = 1000))
        }
        val store = ViewModelStore().apply { put("player", vm) }
    }
}
