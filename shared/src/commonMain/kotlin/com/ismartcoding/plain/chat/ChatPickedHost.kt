package com.ismartcoding.plain.chat

import com.ismartcoding.plain.platform.*
import com.ismartcoding.plain.lib.withIO
import kotlinx.serialization.json.*

object ChatPickedHost {
    private class Stage { var canceled = false }
    private val lock = PlatformLock()
    private val stages = mutableMapOf<String, Stage>()

    suspend fun handle(method: String, params: JsonObject): JsonElement {
        val stage = if (method == "chatPickedRead") Stage().also { slot ->
            lock.withLock { val path = params.getValue("path").jsonPrimitive.content; check(!stages.containsKey(path)); stages[path] = slot }
        } else null
        return withIO {
            if (method == "chatPickedFacts") return@withIO JsonArray(params.getValue("uris").jsonArray.mapNotNull { source ->
                val uri = source.jsonPrimitive.content
                queryPickedFileInfo(uri)?.let { info -> buildJsonObject {
                    put("uri", uri); put("name", info.displayName); put("size", info.size); put("mimeType", info.mimeType)
                } }
            })
            val path = params.getValue("path").jsonPrimitive.content
            if (method == "chatPickedRelease") { release(path); return@withIO JsonPrimitive(true) }
            check(method == "chatPickedRead")
            val stage = checkNotNull(stage)
            try {
                val uri = params.getValue("uri").jsonPrimitive.content
                stagePickedFile(uri, path)
                lock.withLock { check(!stage.canceled) { "Selected file read canceled" } }
                val kind = params.getValue("kind").jsonPrimitive.content
                val size = when (kind) {
                    "image" -> getImageIntrinsicSize(path, getImageRotation(path))
                    "video" -> getVideoIntrinsicSize(path)
                    else -> androidx.compose.ui.unit.IntSize.Zero
                }
                buildJsonObject {
                    put("width", size.width); put("height", size.height); put("durationMs", getMediaDurationMs(path))
                }
            } catch (error: Throwable) { release(path); throw error }
        }
    }
    private fun release(path: String) {
        lock.withLock { stages[path]?.canceled = true }
        deleteFileAt(path)
        check(!fileExists(path)) { "Unable to release selected file" }
        lock.withLock { stages.remove(path) }
    }
    fun disconnect() {
        val paths = lock.withLock { stages.values.forEach { it.canceled = true }; stages.keys.toList() }
        var failure: Throwable? = null
        paths.forEach { path ->
            try { release(path) } catch (error: Throwable) { failure = error }
        }
        failure?.let { throw it }
    }
}
