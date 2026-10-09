package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.db.AudioPlaySource
import com.ismartcoding.plain.features.audio.*
import com.ismartcoding.plain.features.file.FileSortBy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AudioRustHttpTest {
    @Test
    fun playlistsQueueHistoryAndLargeLibraryUseRustWithNativeHostCallbacks() = runBlocking {
        assertEquals(AudioPlaySource.NONE, AudioQueueManager.source().source)
        assertTrue(AudioQueueManager.queuedPaths().isEmpty())
        assertTrue(AudioQueueManager.source().currentPath.isEmpty())
        val prefix = "audio-rust-${UUID.randomUUID()}"
        fun track(index: Int): DPlaylistAudio = DPlaylistAudio("Track $index", "$prefix/$index", "Synthetic $prefix", 117123, "42")
        val previousProvider = AudioLibraryHost.install(object : AudioLibraryProvider {
            override suspend fun count(): Int = 10_000
            override suspend fun page(offset: Int, limit: Int, sort: FileSortBy): List<DPlaylistAudio> =
                (offset until minOf(10_000, offset + limit)).map(::track)
            override suspend fun contains(path: String): Boolean = path.startsWith("$prefix/") && path.substringAfterLast('/').toIntOrNull()?.let { it in 0 until 10_000 } == true
        })
        var playlistId = ""
        val touched = mutableSetOf<String>()
        try {
            val list = AudioPlaylistManager.createPlaylist(prefix)
            playlistId = list.id
            assertEquals(2, AudioPlaylistManager.addPlaylistItems(list.id,listOf(track(1),track(2),track(1))))
            val items = AudioPlaylistManager.playlistItemsPage(list.id,0,20)
            assertEquals(listOf("42","42"), items.map { it.albumId })
            assertEquals(listOf(117123L,117123L), items.map { it.durationMs })
            assertEquals(track(1).path, checkNotNull(AudioQueueManager.setPlaylistSource(list.id,null)).path)
            assertTrue(AudioPlayHistoryManager.recentPageFiltered(prefix,20,0).isEmpty())
            AudioQueueManager.onPlaying(track(1).path,"Track 1", "Synthetic $prefix",117123)
            touched += track(1).path
            assertEquals(1, AudioPlayHistoryManager.recentPageFiltered(prefix,20,0).single().playCount)
            AudioQueueManager.enqueue(listOf(track(2)),true)
            assertEquals(track(2).path, checkNotNull(AudioQueueManager.resolveNext(true,false)).path)
            assertEquals(2,AudioQueueManager.queueTotal())
            AudioQueueManager.setLibrarySource(track(9000).path,false)
            touched += track(9000).path
            assertEquals(10000,AudioQueueManager.queueTotal())
            assertEquals(track(9009).path,AudioQueueManager.queuePage(8990,20).last().path)
            assertEquals(track(9001).path,checkNotNull(AudioQueueManager.resolveNext(true,false)).path)
            touched += track(9001).path
            AudioQueueManager.enqueue(listOf(track(9002)),true)
            assertEquals(track(9002).path,checkNotNull(AudioQueueManager.resolveNext(true,false)).path)
            touched += track(9002).path
            AudioQueueManager.removePaths(listOf(track(1).path))
            assertEquals(1,AudioPlaylistManager.playlistItemCount(list.id))
        } finally {
            try {
                AudioQueueManager.clearQueue()
                AudioQueueManager.removePaths(touched.toList())
                if (playlistId.isNotEmpty()) AudioPlaylistManager.deletePlaylist(playlistId)
            } finally { AudioLibraryHost.install(previousProvider) }
        }
    }
}
