package com.ismartcoding.plain.features.audio

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.models.AudioCommand
import com.ismartcoding.plain.models.AudioItem
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import com.ismartcoding.plain.rust.graphql.ContentSource

object AudioCommands {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    data class Output(val source: ContentSource, val track: AudioItem? = null, val resource: com.ismartcoding.plain.models.ManagedResource? = null)
    private val _output = MutableStateFlow(Output(ContentSource.Local))
    val output = _output.asStateFlow()

    internal fun bindRemoteOutput(source: ContentSource.Remote, track: AudioItem?, resource: com.ismartcoding.plain.models.ManagedResource? = null) {
        _output.value = Output(source, track, resource)
    }
    val changed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    internal fun useLocalSource() {
        _output.value = Output(ContentSource.Local)
    }

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

    suspend fun command(action: AudioCommand, positionMs: Long = 0, speed: Float = 1f) = mutex.withLock {
        val remote = _output.value.source.audioPlayback
        if (remote == null) GraphqlApi.execute(Mutation.audioCommand(action, positionMs, speed))
        else remote.command(action, positionMs, speed)
        changed.tryEmit(Unit)
        Unit
    }

    internal fun submitRemoteCompletion(source: ContentSource.Remote, path: String, revision: Long) = submit {
        mutex.withLock {
            val remote = source.audioPlayback!!
            if (_output.value.source === source && remote.matches(path, revision)) {
                remote.command(AudioCommand.COMPLETED, 0, 1f)
                changed.tryEmit(Unit)
            }
        }
    }

    suspend fun play(audio: AudioItem, source: ContentSource = ContentSource.Local, enqueue: Boolean = false) = mutex.withLock {
        val remote = source.audioPlayback
        if (remote != null) remote.play(audio, enqueue)
        else {
            if (enqueue) source.queue.enqueue(listOf(audio))
            if (!audio.mediaId.isNullOrEmpty()) GraphqlApi.execute(Mutation.audioPlayTrack(audio.mediaId!!))
            else GraphqlApi.execute(Mutation.audioPlayPath(audio.path))
        }
        changed.tryEmit(Unit)
        Unit
    }

    suspend fun clearQueue(source: ContentSource) = mutex.withLock {
        source.queue.clearQueue()
        if (_output.value.source.key == source.key) _output.value.source.audioPlayback?.stop()
        changed.tryEmit(Unit)
        Unit
    }

    suspend fun removeQueued(source: ContentSource, mediaId: String) = mutex.withLock {
        source.queue.removeQueued(mediaId)
        if (_output.value.source.key == source.key && _output.value.track?.mediaId == mediaId) _output.value.source.audioPlayback?.stop()
        changed.tryEmit(Unit)
        Unit
    }

    fun submit(
        action: AudioCommand,
        positionMs: Long = 0,
        speed: Float = 1f,
    ) = submit { command(action, positionMs, speed) }

    fun cycleMode() = submit {
        mutex.withLock {
            val source = _output.value.source
            val modes = MediaPlayMode.entries
            val current = source.queue.playback().mode
            source.api.execute(Mutation.updateAudioPlayMode(modes[(modes.indexOf(current) + 1) % modes.size]))
            changed.tryEmit(Unit)
        }
    }

    fun playTrack(audio: AudioItem, source: ContentSource = ContentSource.Local) = submit { play(audio, source) }

    fun playPath(path: String, source: ContentSource = ContentSource.Local) = submit {
        play(AudioItem(title = "", artist = "", path = path, durationMs = 0), source)
    }
}
