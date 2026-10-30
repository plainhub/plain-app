package com.ismartcoding.plain.rust.host.audio

import com.ismartcoding.plain.models.AudioItem
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.platform.media.AudioPlayer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.*
import kotlin.test.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class AudioEngineHostTest {
    private class Player : AudioPlayer {
        val calls = mutableListOf<String>()
        val gate = CompletableDeferred<Unit>()
        override val isPlayingFlow = MutableStateFlow(false)
        override var currentPath = "song"
        override var progress = 0L
        var lastPositionMs = 0L
        override fun bindRevision(revision: Long) { calls += "bind:$revision" }
        override fun restorePlayback(revision: Long, positionMs: Long) {
            lastPositionMs = positionMs
            bindRevision(revision)
        }
        override suspend fun load(audio: AudioItem, positionMs: Long, speed: Float, revision: Long) {
            calls += "load"
            gate.await()
            currentPath = audio.path
            progress = positionMs
        }
        override suspend fun seekTo(positionMs: Long) {
            calls += "seek"
            gate.await()
            progress = positionMs
        }
        override suspend fun play() { calls += "play" }
        override suspend fun pause() { calls += "pause" }
        override suspend fun clear() { calls += "clear"; currentPath = ""; progress = 0L }
        override suspend fun restartIfPlaying() { calls += "restart" }
        override suspend fun setPlaybackSpeed(speed: Float) { calls += "speed:$speed" }
    }

    private fun command(newTrack: Boolean): JsonObject = JsonHelper.jsonDecode(
        """{"action":"PLAY","newTrack":$newTrack,"speed":1.5,"playback":{"path":"song","positionMs":1200,"revision":5},"track":{"path":"song","title":"Song","artist":"Artist","durationMs":2000}}""",
    )

    @Test
    fun remoteRenderingPreservesLocalSdkFactsAcrossTracksAndLocalRestores() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val player = Player().apply { gate.complete(Unit); progress = 3210 }
        val previous = AudioEngineHost.install(player)
        try {
            AudioEngineHost.command(command(false))
            player.progress = 3210
            AudioEngineHost.renderRemote(AudioItem(path = "https://peer/fs?one", title = "One", artist = "", durationMs = 60000), 10, 0, 1f) {}
            player.progress = 10004
            player.isPlayingFlow.value = true
            assertEquals(com.ismartcoding.plain.rust.host.provider.AudioPlaybackFacts(false, 3210), AudioEngineHost.localPlaybackFacts())
            AudioEngineHost.renderRemote(AudioItem(path = "https://peer/fs?two", title = "Two", artist = "", durationMs = 60000), 11, 0, 1f) {}
            assertEquals(3210, AudioEngineHost.localPlaybackFacts().positionMs)
            val bound = player.calls.last { it.startsWith("bind:") }
            AudioEngineHost.restore(AudioPlaybackState(6, 4321))
            assertEquals(bound, player.calls.last { it.startsWith("bind:") })
            assertEquals(4321, AudioEngineHost.localPlaybackFacts().positionMs)
            AudioEngineHost.command(command(false))
            assertEquals(player.progress, AudioEngineHost.localPlaybackFacts().positionMs)
        } finally {
            AudioEngineHost.command(command(false))
            AudioEngineHost.install(previous)
            Dispatchers.resetMain()
        }
    }

    @Test
    fun resume_acknowledges_only_after_seek_speed_and_play_complete() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val player = Player()
        val previous = AudioEngineHost.install(player)
        try {
            val result = async { AudioEngineHost.command(command(false)) }
            runCurrent()
            assertFalse(result.isCompleted)
            assertEquals(listOf("bind:5", "seek"), player.calls)
            player.gate.complete(Unit)
            val report = JsonHelper.jsonDecode<JsonObject>(result.await().encodeReply(1)).getValue("result").jsonObject
            assertEquals(listOf("bind:5", "seek", "speed:1.5", "play"), player.calls)
            assertEquals(1200, report.getValue("positionMs").jsonPrimitive.int)
            assertEquals(5, report.getValue("revision").jsonPrimitive.int)
            assertFalse(report.getValue("loaded").jsonPrimitive.boolean)
        } finally {
            AudioEngineHost.install(previous)
            Dispatchers.resetMain()
        }
    }

    @Test
    fun loading_waits_for_the_sdk_before_reporting_the_loaded_revision() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val player = Player().apply { currentPath = "other" }
        val previous = AudioEngineHost.install(player)
        try {
            val result = async { AudioEngineHost.command(command(true)) }
            runCurrent()
            assertFalse(result.isCompleted)
            assertEquals(listOf("bind:5", "load"), player.calls)
            player.gate.complete(Unit)
            val report = JsonHelper.jsonDecode<JsonObject>(result.await().encodeReply(1)).getValue("result").jsonObject
            assertEquals("song", report.getValue("path").jsonPrimitive.content)
            assertTrue(report.getValue("loaded").jsonPrimitive.boolean)
        } finally {
            AudioEngineHost.install(previous)
            Dispatchers.resetMain()
        }
    }
    @Test
    fun restored_state_never_overwrites_a_newer_command_or_live_position() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val player = Player()
        player.gate.complete(Unit)
        val previous = AudioEngineHost.install(player)
        try {
            AudioEngineHost.command(command(true))
            player.lastPositionMs = 777
            AudioEngineHost.restore(AudioPlaybackState(4, 0))
            assertEquals(777L, player.lastPositionMs)
            AudioEngineHost.restore(AudioPlaybackState(6, 1234))
            assertEquals(1234L, player.lastPositionMs)
            AudioEngineHost.restore(AudioPlaybackState(6, 0))
            assertEquals(1234L, player.lastPositionMs)
            assertEquals(listOf("bind:5", "load", "bind:6"), player.calls)
        } finally {
            AudioEngineHost.install(previous)
            Dispatchers.resetMain()
        }
    }

    @Test
    fun remote_reports_stay_with_their_rendered_resource_and_stale_callbacks_are_discarded() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val player = Player().apply { gate.complete(Unit) }
        val previous = AudioEngineHost.install(player)
        val first = mutableListOf<AudioHostReport>()
        val second = mutableListOf<AudioHostReport>()
        try {
            val audio = AudioItem(title = "Remote", artist = "", path = "https://fixture.invalid/fs?id=first", durationMs = 1000)
            AudioEngineHost.renderRemote(audio, 7, 0, 1f) { first += it }
            val queued = AudioEngineHost.reportProgress(audio.path, 7, 100)
            AudioEngineHost.renderRemote(audio.copy(path = "https://fixture.invalid/fs?id=second"), 8, 0, 1f) { second += it }
            queued?.join()
            AudioEngineHost.reportProgress(audio.path, 7, 200)?.join()
            AudioEngineHost.reportProgress(player.currentPath, 8, 300)?.join()
            assertEquals(listOf<AudioHostReport>(AudioHostReport.Progress(audio.path, 100, 7)), first)
            assertEquals(listOf<AudioHostReport>(AudioHostReport.Progress(player.currentPath, 300, 8)), second)
            assertEquals("https://fixture.invalid/fs?id=second", player.currentPath)
        } finally {
            AudioEngineHost.command(command(true))
            AudioEngineHost.install(previous)
            Dispatchers.resetMain()
        }
    }

}
