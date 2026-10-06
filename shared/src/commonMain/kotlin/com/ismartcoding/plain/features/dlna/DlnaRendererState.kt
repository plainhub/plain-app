package com.ismartcoding.plain.features.dlna

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.lib.dlna.DlnaMediaType
import com.ismartcoding.plain.lib.dlna.PendingCastRequest
import com.ismartcoding.plain.preferences.UserPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.concurrent.Volatile
import kotlinx.serialization.json.*

enum class DlnaPlaybackState { NO_MEDIA_PRESENT, STOPPED, PLAYING, PAUSED, TRANSITIONING }

/**
 * Host projection of the Rust DLNA receiver.
 *
 * The renderer itself — SSDP advertiser, SOAP dispatch, the allow/deny rules
 * and the cast-request state — lives in plain-rs. What is left here is the
 * Compose-facing view of it: the flows below mirror the engine snapshot, and
 * every mutation goes back through `/dlna/receiver` so the state a control
 * point reads over SOAP is the same state the UI is showing.
 *
 * The flows are only ever written by [apply]; the player reports what it is
 * actually doing through the `reportX` helpers rather than assigning them.
 */
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

    private const val POSITION_PUSH_INTERVAL_MS = 2000L

    private suspend fun command(body: JsonObject) = RustContentApi
        .postJson("dlna/receiver", body)
        .getValue("result")
        .jsonObject
        .let { apply(it) }

    /** Pull the authoritative snapshot. Safe to call at any time. */
    suspend fun refresh() = command(buildJsonObject { put("action", "snapshot") })

    suspend fun start() = command(startBody("start"))

    private fun startBody(action: String) = buildJsonObject {
        put("action", action)
        put("port", UserPrefs.httpPort.value)
    }

    suspend fun stop() = command(buildJsonObject { put("action", "stop") })

    /**
     * Restart after a failed start (a multicast lock the app could not take,
     * for example). Runs on its own scope because stopping tears down the
     * work this coroutine would otherwise be part of.
     */
    fun retry() {
        coIO { command(startBody("retry")) }
    }

    /**
     * Accepts the pending cast: Rust dispatches the media and optionally
     * records the sender so later requests from it are accepted silently.
     */
    fun acceptCastRequest(rememberChoice: Boolean) {
        coIO { command(acceptBody(rememberChoice)) }
    }

    fun rejectCastRequest(rememberChoice: Boolean) {
        coIO { command(acceptBody(rememberChoice, "reject")) }
    }

    private fun acceptBody(remember: Boolean, action: String = "accept") = buildJsonObject {
        put("action", action)
        put("remember", remember)
    }

    /**
     * The player reports its own position so `GetPositionInfo` answers with
     * real numbers. The local flows update on every tick because that is what
     * the UI reads, but the engine only needs the coarse value a control point
     * polls for — pushing every 500ms would be a local HTTP round-trip per
     * frame of the progress bar for no gain.
     */
    fun reportPosition(positionMs: Long, durationMs: Long) {
        currentPositionMs.value = positionMs
        this@DlnaRendererState.durationMs.value = durationMs
        val now = TimeHelper.nowMillis()
        val last = lastPositionPush
        if (last != 0L && now - last < POSITION_PUSH_INTERVAL_MS) return
        lastPositionPush = now
        coIO {
            command(
                buildJsonObject {
                    put("action", "position")
                    put("positionMs", positionMs)
                    put("durationMs", durationMs)
                },
            )
        }
    }

    /** The player has applied the requested seek; stop re-reporting it. */
    fun seekConsumed() {
        coIO { command(buildJsonObject { put("action", "seekConsumed") }) }
    }

    /** What the player is actually doing, which is what GetTransportInfo reports. */
    fun reportPlaybackState(state: DlnaPlaybackState) {
        coIO {
            command(
                buildJsonObject {
                    put("action", "playback")
                    put("state", state.rustName())
                },
            )
        }
    }

    /** The player exited: drop the media and go back to no-media-present. */
    fun reportStopped() {
        coIO { command(buildJsonObject { put("action", "stopped") }) }
    }

    /**
     * The engine names these states the UPnP way (`PausedPlayback`), which is
     * not this enum's spelling — an explicit map beats `valueOf`, which would
     * quietly collapse every unmatched name to no-media-present.
     */
    private fun String.toPlaybackState() = when (this) {
        "Playing" -> DlnaPlaybackState.PLAYING
        "PausedPlayback" -> DlnaPlaybackState.PAUSED
        "Stopped" -> DlnaPlaybackState.STOPPED
        "Transitioning" -> DlnaPlaybackState.TRANSITIONING
        else -> DlnaPlaybackState.NO_MEDIA_PRESENT
    }

    private fun DlnaPlaybackState.rustName() = when (this) {
        DlnaPlaybackState.PLAYING -> "Playing"
        DlnaPlaybackState.PAUSED -> "PausedPlayback"
        DlnaPlaybackState.STOPPED -> "Stopped"
        DlnaPlaybackState.TRANSITIONING -> "Transitioning"
        DlnaPlaybackState.NO_MEDIA_PRESENT -> "NoMediaPresent"
    }

    @Volatile
    private var lastPositionPush = 0L

    fun formatPositionInfo(): Pair<String, String> {
        val pos = currentPositionMs.value
        val dur = durationMs.value
        return Pair(
            TimeHelper.formatTime(pos),
            if (dur > 0) TimeHelper.formatTime(dur) else "00:00:00",
        )
    }

    internal fun apply(row: JsonObject) {
        row["isRunning"]?.jsonPrimitive?.booleanOrNull?.let { isRunning.value = it }
        row["isRetrying"]?.jsonPrimitive?.booleanOrNull?.let { isRetrying.value = it }
        row["mediaUri"]?.jsonPrimitive?.contentOrNull?.let { mediaUri.value = it }
        row["mediaTitle"]?.jsonPrimitive?.contentOrNull?.let { mediaTitle.value = it }
        row["mediaAlbumArtUri"]?.jsonPrimitive?.contentOrNull?.let { mediaAlbumArtUri.value = it }
        row["mediaType"]?.jsonPrimitive?.contentOrNull?.let {
            mediaType.value = runCatching { DlnaMediaType.valueOf(it) }
                .getOrDefault(DlnaMediaType.UNKNOWN)
        }
        row["playbackState"]?.jsonPrimitive?.contentOrNull?.let { name ->
            playbackState.value = name.toPlaybackState()
        }
        row["port"]?.jsonPrimitive?.intOrNull?.let { port.value = it }
        row["currentPositionMs"]?.jsonPrimitive?.longOrNull?.let { currentPositionMs.value = it }
        row["durationMs"]?.jsonPrimitive?.longOrNull?.let { durationMs.value = it }
        row["seekTargetMs"]?.let { value ->
            seekTargetMs.value = if (value is JsonNull) null else value.jsonPrimitive.long
        }
        row["startError"]?.jsonPrimitive?.contentOrNull?.let { startError.value = it }
        val wasPending = pendingCastRequest.value != null
        pendingCastRequest.value = row["pendingCastRequest"]?.let { value ->
            if (value is JsonNull) {
                null
            } else {
                val item = value.jsonObject
                PendingCastRequest(
                    senderIp = item["senderIp"]?.jsonPrimitive?.content.orEmpty(),
                    senderName = item["senderName"]?.jsonPrimitive?.content.orEmpty(),
                    mediaUri = item["mediaUri"]?.jsonPrimitive?.content.orEmpty(),
                    mediaTitle = item["mediaTitle"]?.jsonPrimitive?.content.orEmpty(),
                    mediaType = runCatching {
                        DlnaMediaType.valueOf(item["mediaType"]?.jsonPrimitive?.content.orEmpty())
                    }.getOrDefault(DlnaMediaType.UNKNOWN),
                    albumArtUri = item["albumArtUri"]?.jsonPrimitive?.content.orEmpty(),
                )
            }
        }
        // A cast that arrives while the app is in the background has to bring
        // it forward, or playback waits for the user to reopen it themselves.
        if (!wasPending && pendingCastRequest.value != null) {
            sendEvent(DlnaCastRequestEvent())
        }
    }
}
