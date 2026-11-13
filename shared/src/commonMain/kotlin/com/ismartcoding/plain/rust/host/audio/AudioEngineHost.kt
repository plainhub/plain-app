package com.ismartcoding.plain.rust.host.audio

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.features.audio.AudioCommands
import com.ismartcoding.plain.models.AudioCommand
import com.ismartcoding.plain.models.AudioItem
import com.ismartcoding.plain.platform.media.AudioPlayer
import com.ismartcoding.plain.platform.media.audioPlayer
import com.ismartcoding.plain.rust.host.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
private data class AudioEngineCommand(
    val action: String,
    val playback: AudioPlaybackState,
    val speed: Float,
    val newTrack: Boolean,
    val track: AudioItem? = null,
)

@Serializable
internal data class AudioPlaybackState(val revision: Long, val positionMs: Long)

@Serializable
internal data class AudioSelectionState(val path: String, val revision: Long, val positionMs: Long)

object AudioEngineHost {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutex = Mutex()
    private var overridePlayer: AudioPlayer? = null
    private var revision = -1L
    private var localRevision = -1L
    private var stagedSelection: AudioSelectionState? = null
    private var reporter: Job? = null
    private var pendingReport: Job? = null
    private var remoteReport: RemoteReport? = null

    private class RemoteReport(val path: String, val revision: Long, val send: suspend (AudioHostReport) -> Unit)

    internal suspend fun renderRemote(track: AudioItem, requestedRevision: Long, positionMs: Long, speed: Float, report: suspend (AudioHostReport) -> Unit) = withContext(Dispatchers.Main) {
        mutex.withLock {
            val player = engine()
            player.pause()
            remoteReport = RemoteReport(track.path, requestedRevision, report)
            revision = requestedRevision
            player.load(track, positionMs, speed, requestedRevision)
        }
    }

    internal suspend fun stageLocalSelection(selection: AudioSelectionState) = withContext(Dispatchers.Main) {
        require(selection.path.isNotEmpty() && selection.revision >= 0 && selection.positionMs >= 0)
        mutex.withLock {
            check(selection.revision > maxOf(localRevision, stagedSelection?.revision ?: -1)) { "Stale audio selection" }
            stagedSelection = selection
        }
    }

    internal suspend fun renderLocal(track: AudioItem, speed: Float) = withContext(Dispatchers.Main) {
        require(speed.isFinite() && speed > 0)
        mutex.withLock {
            val selection = checkNotNull(stagedSelection) { "Audio selection is not staged" }
            check(selection.path == track.path && selection.revision > localRevision) { "Stale audio selection" }
            stagedSelection = null
            val player = engine()
            useLocal(player)
            revision = selection.revision
            localRevision = selection.revision
            player.load(track, selection.positionMs, speed, selection.revision)
        }
    }

    private suspend fun useLocal(player: AudioPlayer) {
        if (remoteReport != null) {
            player.pause()
            remoteReport = null
        }
        AudioCommands.useLocalSource()
    }

    internal suspend fun controlLocal(action: AudioCommand, positionMs: Long, speed: Float) = withContext(Dispatchers.Main) {
        require(positionMs >= 0 && speed.isFinite() && speed > 0)
        val progress = mutex.withLock {
            val player = engine()
            check(remoteReport == null) { "Local audio is not active" }
            control(player, action, positionMs, speed)
            when (action) {
                AudioCommand.SEEK -> AudioHostReport.Progress(player.currentPath, positionMs, revision)
                AudioCommand.PAUSE -> sampleProgress(player)
                else -> null
            }
        }
        if (progress != null) reportProgress(progress.path, progress.revision, progress.positionMs)?.join()
    }

    internal suspend fun controlRemote(action: AudioCommand, positionMs: Long, speed: Float) = withContext(Dispatchers.Main) {
        mutex.withLock { control(engine(), action, positionMs, speed) }
    }

    private suspend fun control(player: AudioPlayer, action: AudioCommand, positionMs: Long, speed: Float) {
        when (action) {
            AudioCommand.PLAY -> player.play()
            AudioCommand.PAUSE -> player.pause()
            AudioCommand.SEEK -> player.seekTo(positionMs)
            AudioCommand.CLEAR -> player.clear()
            AudioCommand.RESTART -> player.restartIfPlaying()
            AudioCommand.SET_SPEED -> player.setPlaybackSpeed(speed)
            else -> error("Audio selection must be resolved before rendering")
        }
    }

    suspend fun install(player: AudioPlayer?): AudioPlayer? = withContext(Dispatchers.Main) {
        mutex.withLock {
            val old = overridePlayer
            overridePlayer = player
            old
        }
    }
    private fun engine(): AudioPlayer = overridePlayer ?: audioPlayer

    internal fun startProgressReporting() {
        if (reporter != null) return
        reporter = scope.launch {
            while (isActive) {
                delay(1_000)
                try {
                    val progress = mutex.withLock { sampleProgress(engine()) }
                    reportProgress(progress.path, progress.revision, progress.positionMs)?.join()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    LogCat.e("Audio progress: ${error.message}")
                }
            }
        }
    }

    internal fun stopProgressReporting() {
        reporter?.cancel()
        reporter = null
    }

    internal fun stopProgressReporting(path: String, revision: Long, positionMs: Long) {
        stopProgressReporting()
        reportProgress(path, revision, positionMs)
    }

    internal fun reportStarted(path: String, revision: Long) {
        enqueueReport(AudioHostReport.Started(path, revision))
    }

    internal fun reportCompleted(path: String, revision: Long, positionMs: Long) {
        stopProgressReporting(path, revision, positionMs)
        enqueueReport(AudioHostReport.Completed(path, revision))
    }

    internal fun reportProgress(path: String, revision: Long, positionMs: Long): Job? {
        if (path.isEmpty() || revision < 0) return null
        return enqueueReport(AudioHostReport.Progress(path, positionMs.coerceAtLeast(0), revision))
    }

    private fun enqueueReport(report: AudioHostReport): Job {
        val previous = pendingReport
        val target = remoteReport
        return scope.launch {
            previous?.join()
            try {
                if (target == null) {
                    if (remoteReport == null && revision == report.revision) report.sendToRust()
                } else if (remoteReport === target && target.path == report.path && target.revision == report.revision) {
                    target.send(report)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                LogCat.e("Audio report ${report::class.simpleName}: ${error.message}")
            }
        }.also { pendingReport = it }
    }

    internal suspend fun command(params: JsonObject): HostResult = withContext(Dispatchers.Main) {
        mutex.withLock {
            val player = engine()
            val command = JsonHelper.jsonDecodeFromElement<AudioEngineCommand>(params)
            useLocal(player)
            val requestedRevision = command.playback.revision
            val position = command.playback.positionMs
            val speed = command.speed
            require(position >= 0 && requestedRevision >= 0 && speed.isFinite() && speed > 0)
            val action = command.action
            var loaded = false
            when (action) {
                "PLAY", "SEEK" -> {
                    val track = requireNotNull(command.track) { "Audio track required for $action" }
                    if (action == "PLAY" && !command.newTrack && player.currentPath == track.path) {
                        player.bindRevision(requestedRevision)
                        player.seekTo(position)
                        player.setPlaybackSpeed(speed)
                        player.play()
                    } else if (action == "SEEK" && player.currentPath == track.path) {
                        player.bindRevision(requestedRevision)
                        player.seekTo(position)
                    } else { player.load(track, position, speed, requestedRevision); loaded = true }
                }
                else -> {
                    val actionType = AudioCommand.valueOf(action)
                    player.bindRevision(requestedRevision)
                    control(player, actionType, position, speed)
                }
            }
            revision = requestedRevision
            localRevision = requestedRevision
            val reported = sampleProgress(player)
            hostResult(AudioEngineReport(
                reported.path, if (action == "PLAY" || action == "SEEK") position else reported.positionMs, reported.revision, loaded,
            ))
        }
    }
    private fun sampleProgress(player: AudioPlayer) = AudioHostReport.Progress(player.currentPath, player.progress.coerceAtLeast(0), revision)
}

@Serializable
private data class AudioEngineReport(val path: String, val positionMs: Long, val revision: Long, val loaded: Boolean)
