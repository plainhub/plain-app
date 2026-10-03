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
            else -> error("Unknown file host method")
        }
        JsonPrimitive(true)
    }
    private fun isPrivate(path: String): Boolean = listOf(appDir(),cacheDirPath()).filter { it.isNotEmpty() }.map(::getCanonicalPath).any { path == it || path.startsWith("$it/") }
}
