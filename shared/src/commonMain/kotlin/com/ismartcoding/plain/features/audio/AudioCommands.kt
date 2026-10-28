package com.ismartcoding.plain.features.audio

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.models.AudioCommand
import com.ismartcoding.plain.models.AudioItem
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

object AudioCommands {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val requests = Channel<suspend () -> Unit>(32)

    init {
        scope.launch {
            for (request in requests) {
                try { request() }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (e: Exception) { LogCat.e("Audio command: ${e.message}") }
            }
        }
    }

    private fun submit(request: suspend () -> Unit) {
        check(requests.trySend(request).isSuccess) { "Audio command capacity exceeded" }
    }

    suspend fun command(action: AudioCommand, positionMs: Long = 0, speed: Float = 1f) {
        GraphqlApi.execute(Mutation.audioCommand(action, positionMs, speed))
    }

    fun submit(
        action: AudioCommand,
        positionMs: Long = 0,
        speed: Float = 1f,
    ) = submit { command(action, positionMs, speed) }

    fun cycleMode() = submit {
        val current = AudioQueueClient.playback().mode
        val modes = MediaPlayMode.entries
        val next = modes[(modes.indexOf(current) + 1) % modes.size]
        GraphqlApi.execute(Mutation.updateAudioPlayMode(next))
    }

    fun playTrack(audio: AudioItem) = submit {
        if (!audio.mediaId.isNullOrEmpty()) {
            GraphqlApi.execute(Mutation.audioPlayTrack(audio.mediaId!!))
        } else {
            GraphqlApi.execute(Mutation.audioPlayPath(audio.path))
        }
    }

    fun playPath(path: String) = submit {
        GraphqlApi.execute(Mutation.audioPlayPath(path))
    }
}
