package com.ismartcoding.plain.tests

import com.ismartcoding.plain.graphql.Query
import java.util.UUID
import com.ismartcoding.plain.rust.graphql.GraphqlRequest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.features.audio.*
import com.ismartcoding.plain.models.AudioCommand
import com.ismartcoding.plain.rust.host.audio.AudioEngineHost
import com.ismartcoding.plain.ui.audio.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import com.ismartcoding.plain.rust.graphql.GraphqlApi

@RunWith(AndroidJUnit4::class)
class AudioNativeRustHttpTest {
    @Test
    fun silentNativePlaybackReportsActualStartAndResumeThroughRust() = runBlocking {
        assertTrue(AudioQueueClient.queuedMediaIds().isEmpty())
        assertTrue(AudioQueueClient.playback().path.isNullOrEmpty())
        val file = File(appContext.cacheDir, "audio-native-${UUID.randomUUID()}.wav")
        val samples = 8_000 * 20 * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            .put("RIFF".toByteArray()).putInt(samples + 36).put("WAVEfmt ".toByteArray()).putInt(16)
            .putShort(1).putShort(1).putInt(8_000).putInt(16_000).putShort(2).putShort(16)
            .put("data".toByteArray()).putInt(samples).array()
        file.outputStream().use { it.write(header); it.write(ByteArray(samples)) }
        val oldEngine = AudioEngineHost.install(null)
        suspend fun playback() = GraphqlApi.execute(GraphqlRequest<JsonObject>("query { audioPlayback { path positionMs revision } }")).getValue("audioPlayback").jsonObject
        try {
            GraphqlApi.execute(GraphqlRequest<JsonObject>("mutation { audioPlayPath(path: ${JsonPrimitive(file.absolutePath)}) {path positionMs revision} }"))
            assertTrue(GraphqlApi.execute(Query.mediaPlayHistory("type:AUDIO text:${JsonPrimitive(file.name)}", 0, 20)).isEmpty())
            assertTrue(AudioQueueClient.queuedMediaIds().isEmpty())
            assertNull(AudioQueueClient.playback().mediaId)
            withTimeout(5_000) {
                while (playback().getValue("positionMs").jsonPrimitive.long < 100) delay(100)
            }
            AudioCommands.command(AudioCommand.SEEK,5_000)
            delay(100)
            AudioCommands.command(AudioCommand.PAUSE)
            val paused = playback().getValue("positionMs").jsonPrimitive.long
            assertTrue("Native seek must be persisted", paused in 4_900..6_000)
            AudioCommands.command(AudioCommand.PLAY)
            delay(300)
            assertTrue(playback().getValue("positionMs").jsonPrimitive.long >= paused)
            assertTrue(GraphqlApi.execute(Query.mediaPlayHistory("type:AUDIO text:${JsonPrimitive(file.name)}", 0, 20)).isEmpty())
        } finally {
            try {
                AudioCommands.command(AudioCommand.CLEAR)
                AudioQueueClient.clearQueue()
            } finally {
                AudioEngineHost.install(oldEngine)
                file.delete()
            }
        }
    }
}
