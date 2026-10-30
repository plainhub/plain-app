package com.ismartcoding.plain.features.audio

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.models.AudioCommand
import com.ismartcoding.plain.models.AudioItem
import com.ismartcoding.plain.models.AudioPlayback
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.rust.host.audio.AudioEngineHost
import com.ismartcoding.plain.rust.host.audio.AudioHostReport
import kotlinx.serialization.json.JsonPrimitive

internal class RemoteAudioPlayback(private val source: ContentSource.Remote) {
    private var selection: AudioPlayback? = null
    private var track: AudioItem? = null

    fun matches(path: String, revision: Long): Boolean = track?.path == path && selection?.revision == revision

    suspend fun play(audio: AudioItem, enqueue: Boolean) {
        val item = audio.mediaId?.let { source.queue.select(it, enqueue) } ?: source.media.audioInfo(audio.path)
        val settings = source.queue.playbackSettings()
        val playback = settings.audioPlayback
        render(item, if (item.mediaId == null) playback.copy(path = item.path, mediaId = null, positionMs = 0) else playback, settings.speed)
    }

    private suspend fun render(item: AudioItem, playback: AudioPlayback, speed: Float) {
        val resource = source.client.resource(item.path)
        selection = playback
        track = item
        AudioCommands.bindRemoteOutput(source, item, resource)
        AudioEngineHost.renderRemote(item.copy(path = resource.url), playback.revision, playback.positionMs, speed) { report ->
            when (report) {
                is AudioHostReport.Started -> item.mediaId?.let { source.history.recordPlay(it, DataType.AUDIO, 0).join() }
                is AudioHostReport.Progress -> item.mediaId?.let { source.history.saveProgress(it, DataType.AUDIO, report.positionMs).join() }
                is AudioHostReport.Completed -> AudioCommands.submitRemoteCompletion(source, item.path, playback.revision)
            }
        }
    }

    suspend fun stop() {
        AudioEngineHost.controlRemote(AudioCommand.CLEAR, 0, 1f)
        track = null
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
                val selectionAction = when (action) {
                    AudioCommand.PREVIOUS -> AudioSelectionAction.PREVIOUS
                    AudioCommand.COMPLETED -> AudioSelectionAction.COMPLETED
                    else -> AudioSelectionAction.NEXT
                }
                val next = source.api.execute(Mutation.advanceAudioSelection(selectionAction, current.path.orEmpty(), current.revision, false))
                    ?: return
                if (next.track == null) {
                    stop()
                } else render(next.track, current.copy(path = next.path, mediaId = next.track.mediaId, positionMs = next.positionMs, revision = next.revision), source.queue.playbackSpeed())
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
