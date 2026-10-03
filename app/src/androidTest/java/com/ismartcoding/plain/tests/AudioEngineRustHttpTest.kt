package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.api.*
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.features.audio.*
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.enums.MediaPlayMode
import com.ismartcoding.plain.preferences.UserPrefs
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AudioEngineRustHttpTest {
    @Test
    fun commandsOwnResumeCompletionAndRejectStalePhysicalReports() = runBlocking {
        assertTrue(AudioQueueManager.queuedPaths().isEmpty())
        assertTrue(AudioQueueManager.source().currentPath.isEmpty())
        val prefix = "audio-engine-${UUID.randomUUID()}"
        fun track(index: Int) = DPlaylistAudio("Track $index", "$prefix/$index", "Synthetic", 117123, "42")
        val oldProvider = AudioLibraryHost.install(object : AudioLibraryProvider {
            override suspend fun count() = 2
            override suspend fun page(offset: Int, limit: Int, sort: FileSortBy) = (offset until minOf(2, offset + limit)).map(::track)
            override suspend fun contains(path: String) = path == track(0).path || path == track(1).path
            override suspend fun metadata(path: String) = track(path.substringAfterLast('/').toInt())
        })
        val engine = AudioTestPlayer()
        val oldEngine = AudioEngineHost.install(engine)
        val oldMode = UserPrefs.audioPlayMode.value
        try {
            AudioQueueManager.enqueue(listOf(track(0),track(1)))
            val played = RustContentApi.mutate("audioPlayTrack(track: {path:${JsonPrimitive(track(0).path)},title:\"Track 0\",artist:\"Synthetic\",albumId:\"42\",durationMs:117123},enqueue:false) {path revision positionMs}").getValue("audioPlayTrack").jsonObject
            assertEquals(track(0).path, played.getValue("path").jsonPrimitive.content)
            val initialRevision = played.getValue("revision").jsonPrimitive.long
            AudioQueueManager.onStarted(track(0), initialRevision)
            AudioQueueManager.onStarted(track(0), initialRevision)
            assertEquals(1, AudioPlayHistoryManager.recentPageFiltered(prefix,20,0).single().playCount)
            AudioCommands.command("SEEK", 3_000_000_123)
            val stale = RustContentApi.mutate("audioReportProgress(path:${JsonPrimitive(track(0).path)},revision:$initialRevision,positionMs:0)")
            assertFalse(stale.getValue("audioReportProgress").jsonPrimitive.boolean)
            AudioCommands.command("PAUSE")
            AudioCommands.command("PLAY")
            assertEquals(3_000_000_123, engine.progress)
            assertEquals(1, engine.loads)
            UserPrefs.audioPlayMode.value = MediaPlayMode.REPEAT_ONE
            AudioCommands.command("COMPLETED")
            assertEquals(track(0).path, engine.currentPath)
            assertEquals(0, engine.progress)
            assertEquals(2, engine.loads)
            AudioCommands.command("NEXT")
            assertEquals(track(1).path, engine.currentPath)
            engine.fail = true
            var failed = false
            try { AudioCommands.command("PAUSE") } catch (_: Exception) { failed = true }
            assertTrue("Native player failures must propagate", failed)
            engine.fail = false
        } finally {
            try {
                engine.fail = false
                AudioCommands.command("CLEAR")
                AudioQueueManager.clearQueue()
                AudioQueueManager.removePaths(listOf(track(0).path,track(1).path))
                UserPrefs.audioPlayMode.value = oldMode
            } finally {
                AudioEngineHost.install(oldEngine)
                AudioLibraryHost.install(oldProvider)
            }
        }
    }
}
