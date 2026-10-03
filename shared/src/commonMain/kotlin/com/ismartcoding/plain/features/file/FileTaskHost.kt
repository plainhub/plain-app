package com.ismartcoding.plain.features.file

import com.ismartcoding.plain.helpers.FilePathValidator
import com.ismartcoding.plain.platform.*
import com.ismartcoding.plain.lib.withIO
import kotlinx.serialization.json.*

object FileTaskHost {
    suspend fun handle(method: String, params: JsonObject): JsonElement = withIO {
        val paths = params.getValue("paths").jsonArray.map { it.jsonPrimitive.content }
        when (method) {
            "fileTaskAuthorize" -> {
                val canonical = paths.map(::getCanonicalPath)
                FilePathValidator.requireAllSafe(canonical.filterNot(::isPrivate))
                if (canonical.any { !isPrivate(it) }) check(Permission.WRITE_EXTERNAL_STORAGE.isGranted()) { "File access permission denied" }
            }
            "fileTaskScan" -> scanFileTaskPaths(paths.filterNot { isPrivate(getCanonicalPath(it)) })
            "fileTaskMediaSnapshot" -> {
                require(paths.size <= 128)
                val external = paths.filterNot { isPrivate(getCanonicalPath(it)) }
                FilePathValidator.requireAllSafe(external.map(::getCanonicalPath))
                return@withIO buildJsonArray {
                    queryFileTaskMedia(external).forEach { row -> add(buildJsonObject {
                        put("mediaType", row.type.value); put("mediaId", row.id); put("path", row.path)
                    }) }
                }
            }
            else -> error("Unknown file host method")
        }
        JsonPrimitive(true)
    }
    private fun isPrivate(path: String): Boolean = listOf(appDir(),cacheDirPath()).filter { it.isNotEmpty() }.map(::getCanonicalPath).any { path == it || path.startsWith("$it/") }
}
