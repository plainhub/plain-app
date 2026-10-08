package com.ismartcoding.plain.features.dlna

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.lib.dlna.DlnaMediaType
import com.ismartcoding.plain.lib.dlna.PendingCastRequest
import com.ismartcoding.plain.preferences.UserPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.*

object DlnaRendererState {
    /** Broadcast by the engine whenever the renderer state changes. */
    const val EVENT_UPDATED = 10005

    val isRunning = MutableStateFlow(false)
    val isRetrying = MutableStateFlow(false)
    val mediaUri = MutableStateFlow("")
    val mediaTitle = MutableStateFlow("")
    val mediaAlbumArtUri = MutableStateFlow("")
    val mediaType = MutableStateFlow(DlnaMediaType.UNKNOWN)
    val playbackState = MutableStateFlow(DlnaPlaybackState.NO_MEDIA_PRESENT)
    val port = MutableStateFlow(7878)
    val currentPositionMs = MutableStateFlow(0L)
    val durationMs = MutableStateFlow(0L)
    /** Non-null signals the player to seek to this position (milliseconds). */
    val seekTargetMs = MutableStateFlow<Long?>(null)
    /** Non-null when a cast request is shown to the user for confirmation. */
    val pendingCastRequest = MutableStateFlow<PendingCastRequest?>(null)
    /** Non-empty when the receiver failed to start on all attempted ports. */
    val startError = MutableStateFlow("")

    private val projectionLock = com.ismartcoding.plain.platform.PlatformLock()
    private var version = -1L

    private suspend fun command(command: ReceiverCommand) {
        val response = RustContentApi.postJsonOrThrow("dlna/receiver", JsonHelper.jsonEncodeToElement<ReceiverCommand>(command).jsonObject)
        apply(response.getValue("result").jsonObject)
    }
    suspend fun refresh() = command(ReceiverCommand.Snapshot)
    suspend fun start() = command(ReceiverCommand.Start(UserPrefs.httpPort.value))
    suspend fun stop() = command(ReceiverCommand.Stop)
    fun retry() { coIO { command(ReceiverCommand.Retry(UserPrefs.httpPort.value)) } }
    fun acceptCastRequest(rememberChoice: Boolean) { coIO { command(ReceiverCommand.Accept(rememberChoice)) } }
    fun rejectCastRequest(rememberChoice: Boolean) { coIO { command(ReceiverCommand.Reject(rememberChoice)) } }
    fun reportPosition(positionMs: Long, durationMs: Long) {
        coIO { command(ReceiverCommand.Position(positionMs, durationMs)) }
    }
    fun seekConsumed() { coIO { command(ReceiverCommand.SeekConsumed) } }
    fun reportPlaybackState(state: DlnaPlaybackState) { coIO { command(ReceiverCommand.Playback(state)) } }
    fun reportStopped() { coIO { command(ReceiverCommand.Stopped) } }

    internal fun apply(row: JsonObject) {
        val state = JsonHelper.jsonDecodeFromElement<ReceiverSnapshot>(row)
        projectionLock.withLock {
            if (state.version < version) return@withLock
            version = state.version
            isRunning.value = state.isRunning
            isRetrying.value = state.isRetrying
            mediaUri.value = state.mediaUri
            mediaTitle.value = state.mediaTitle
            mediaAlbumArtUri.value = state.mediaAlbumArtUri
            mediaType.value = state.mediaType
            playbackState.value = state.playbackState
            port.value = state.port
            currentPositionMs.value = state.currentPositionMs
            durationMs.value = state.durationMs
            seekTargetMs.value = state.seekTargetMs
            startError.value = state.startError
            val wasPending = pendingCastRequest.value != null
            pendingCastRequest.value = state.pendingCastRequest
            if (!wasPending && state.pendingCastRequest != null) sendEvent(DlnaCastRequestEvent())
        }
    }
}
