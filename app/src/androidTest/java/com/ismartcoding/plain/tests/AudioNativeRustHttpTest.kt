package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.features.audio.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AudioNativeRustHttpTest {
    @Test
    fun silentNativePlaybackReportsActualStartAndResumeThroughRust() = runBlocking {
        assertTrue(AudioQueueManager.queuedPaths().isEmpty())
        assertTrue(AudioQueueManager.source().currentPath.isEmpty())
        val file = File(appContext.cacheDir, "audio-native-${UUID.randomUUID()}.wav")
        val samples = 8_000 * 20 * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            .put("RIFF".toByteArray()).putInt(samples + 36).put("WAVEfmt ".toByteArray()).putInt(16)
            .putShort(1).putShort(1).putInt(8_000).putInt(16_000).putShort(2).putShort(16)
            .put("data".toByteArray()).putInt(samples).array()
        file.outputStream().use { it.write(header); it.write(ByteArray(samples)) }
        val oldEngine = AudioEngineHost.install(null)
        suspend fun playback() = RustContentApi.query("audioPlayback { path positionMs revision }").getValue("audioPlayback").jsonObject
        try {
            RustContentApi.mutate("audioPlayPath(path: ${JsonPrimitive(file.absolutePath)}) {path positionMs revision}")
            withTimeout(10_000) {
                while (AudioPlayHistoryManager.recentPageFiltered(file.name,20,0).isEmpty()) delay(100)
            }
            assertEquals(1, AudioPlayHistoryManager.recentPageFiltered(file.name,20,0).single().playCount)
            withTimeout(5_000) {
                while (playback().getValue("positionMs").jsonPrimitive.long < 100) delay(100)
            }
            AudioCommands.command("SEEK",5_000)
            delay(100)
            AudioCommands.command("PAUSE")
            val paused = playback().getValue("positionMs").jsonPrimitive.long
            assertTrue("Native seek must be persisted", paused in 4_900..6_000)
            AudioCommands.command("PLAY")
            delay(300)
            assertTrue(playback().getValue("positionMs").jsonPrimitive.long >= paused)
            assertEquals(1, AudioPlayHistoryManager.recentPageFiltered(file.name,20,0).single().playCount)
        } finally {
            try {
                AudioCommands.command("CLEAR")
                AudioQueueManager.clearQueue()
                AudioQueueManager.removePaths(listOf(file.absolutePath))
            } finally {
                AudioEngineHost.install(oldEngine)
                file.delete()
            }
        }
    }
}
