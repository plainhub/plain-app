package com.ismartcoding.plain.features.audio

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.models.AudioCommand
import com.ismartcoding.plain.models.AudioItem
import com.ismartcoding.plain.models.AudioPlayback
import com.ismartcoding.plain.models.PlaybackType
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.rust.host.audio.AudioEngineHost
import com.ismartcoding.plain.rust.host.audio.AudioHostReport
import kotlinx.serialization.json.JsonPrimitive

internal class RemoteAudioPlayback(private val source: ContentSource.Remote) {
    private var selection: AudioPlayback? = null
    private var playGeneration = 0L
    private var track: AudioItem? = null

    fun matches(path: String, revision: Long): Boolean = track?.path == path && playGeneration == revision

    suspend fun play(audio: AudioItem, enqueue: Boolean) {
        val item = audio.mediaId?.let { source.queue.select(it, enqueue) } ?: source.media.audioInfo(audio.path)
        val settings = source.queue.playbackSettings()
        val playback = settings.audioPlayback
        if (item.mediaId != null) {
            check(playback.mediaId == item.mediaId && playback.path == item.path) { "Audio selection changed before playback" }
        }
        render(item, playback, settings.speed)
    }

    private suspend fun render(item: AudioItem, playback: AudioPlayback, speed: Float) {
        val resource = source.client.resource(item.path)
        val generation = ++playGeneration
        selection = playback
        track = item
        AudioCommands.bindRemoteOutput(source, item, resource)
        AudioEngineHost.renderRemote(item.copy(path = resource.url), generation, 0, speed) { report ->
            when (report) {
                is AudioHostReport.Started -> item.mediaId?.let { source.history.recordPlay(it, PlaybackType.AUDIO, 0).join() }
                is AudioHostReport.Progress -> item.mediaId?.let { source.history.saveProgress(it, PlaybackType.AUDIO, report.positionMs).join() }
                is AudioHostReport.Completed -> AudioCommands.submitRemoteCompletion(source, item.path, generation)
            }
        }
    }

    suspend fun stop() {
        AudioEngineHost.controlRemote(AudioCommand.CLEAR, 0, 1f)
        track = null
        selection = null
        AudioCommands.bindRemoteOutput(source, null)
    }

    suspend fun command(action: AudioCommand, positionMs: Long, speed: Float) {
        when (action) {
            AudioCommand.NEXT, AudioCommand.PREVIOUS, AudioCommand.COMPLETED -> {
                val current = selection ?: return
                if (track?.mediaId == null) {
                    AudioEngineHost.controlRemote(AudioCommand.PAUSE, 0, 1f)
                    return
                }
                val snapshot = source.queue.snapshot(1)
                if (snapshot.audioPlayback != current) return
                val next = source.queue.nextMediaId(snapshot, action)
                if (next == null) stop()
                else {
                    val item = source.queue.select(next, enqueue = false)
                    val settings = source.queue.playbackSettings()
                    if (settings.audioPlayback.mediaId == item.mediaId && settings.audioPlayback.path == item.path) {
                        render(item, settings.audioPlayback, settings.speed)
                    }
                }
            }
            AudioCommand.CLEAR -> {
                source.queue.clearQueue()
                stop()
            }
            AudioCommand.SET_SPEED -> {
                source.api.execute(Mutation.setPref("audio_playback_speed", JsonPrimitive(speed)))
                AudioEngineHost.controlRemote(action, positionMs, speed)
            }
            else -> AudioEngineHost.controlRemote(action, positionMs, speed)
        }
    }
}
