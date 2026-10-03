package com.ismartcoding.plain.features.audio

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.api.*
import com.ismartcoding.plain.platform.AudioPlayer
import com.ismartcoding.plain.platform.audioPlayer
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*

object AudioEngineHost {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private var overridePlayer: AudioPlayer? = null
    private var revision = -1L
    private var reporter: Job? = null

    suspend fun install(player: AudioPlayer?): AudioPlayer? = withContext(Dispatchers.Main) {
        mutex.withLock {
            val old = overridePlayer
            overridePlayer = player
            old
        }
    }
    private fun engine(): AudioPlayer = overridePlayer ?: audioPlayer

    suspend fun start() {
        check(reporter == null) { "Audio progress reporter already started" }
        val restored = RustContentApi.mutate("audioInvalidateEngine { path positionMs revision }").getValue("audioInvalidateEngine").jsonObject
        withContext(Dispatchers.Main) {
                mutex.withLock {
                    revision = restored.getValue("revision").jsonPrimitive.long
                    TempData.audioPlayPosition = restored.getValue("positionMs").jsonPrimitive.long
                }
            }
        reporter = scope.launch {
            while (isActive) {
                delay(1_000)
                try {
                    val report = withContext(Dispatchers.Main) { mutex.withLock { report(engine()) } }
                    val path = report.string("path")
                    if (path.isNotEmpty() && report.getValue("revision").jsonPrimitive.long >= 0) {
                        RustContentApi.mutate("audioReportProgress(path: ${gql(path)}, revision: ${report.getValue("revision")}, positionMs: ${report.getValue("positionMs")})")
                    }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (e: Exception) { LogCat.e("Audio progress: ${e.message}") }
            }
        }
    }
    suspend fun handle(method: String, params: JsonObject): JsonElement = when (method) {
        "audioEngineCommand" -> withContext(Dispatchers.Main) {
            mutex.withLock {
                val player = engine()
                val playback = params.getValue("playback").jsonObject
                val requestedRevision = playback.getValue("revision").jsonPrimitive.long
                val position = playback.getValue("positionMs").jsonPrimitive.long
                val speed = params.getValue("speed").jsonPrimitive.float
                require(position >= 0 && requestedRevision >= 0 && speed.isFinite() && speed > 0)
                val action = params.string("action")
                var loaded = false
                when (action) {
                    "PLAY", "SEEK" -> {
                        val track = params.getValue("track").audioTrack()
                        if (action == "PLAY" && !params.getValue("newTrack").jsonPrimitive.boolean && player.currentPath == track.path) {
                            player.seekTo(position)
                            player.play()
                            player.setPlaybackSpeed(speed)
                        } else if (action == "SEEK" && player.currentPath == track.path) {
                            player.seekTo(position)
                        } else { player.load(track, position, speed, requestedRevision); loaded = true }
                    }
                    "PAUSE" -> player.pause()
                    "CLEAR" -> player.clear()
                    "RESTART" -> player.restartIfPlaying()
                    "SET_SPEED" -> player.setPlaybackSpeed(speed)
                    else -> error("Unknown audio engine command: $action")
                }
                yield()
                revision = requestedRevision
                JsonObject(report(player) + ("loaded" to JsonPrimitive(loaded))).let { reported -> if (action == "PLAY" || action == "SEEK") JsonObject(reported + ("positionMs" to JsonPrimitive(position))) else reported }
            }
        }
        else -> error("Unknown audio engine method: $method")
    }
    private fun report(player: AudioPlayer): JsonObject = buildJsonObject {
        put("path", player.currentPath); put("positionMs", player.progress.coerceAtLeast(0)); put("revision", revision)
    }
}
