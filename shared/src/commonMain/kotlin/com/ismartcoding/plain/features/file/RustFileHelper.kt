package com.ismartcoding.plain.features.file

import com.ismartcoding.plain.api.RustContentApi
import io.ktor.http.Url
import io.ktor.http.decodeURLPart
import kotlinx.serialization.json.*
import kotlin.time.Instant

object RustFileHelper {
    suspend fun createDirectory(path: String): DFile = write("createDirectory", path)
    suspend fun createFile(path: String): DFile = write("createFile", path)
    suspend fun writeText(path: String, content: String, overwrite: Boolean): DFile = write("writeText", path, content, overwrite)

    private suspend fun write(action: String, path: String, content: String = "", overwrite: Boolean = false): DFile {
        val row = RustContentApi.postJson("files/write", buildJsonObject {
            put("action", action)
            put("path", if (path.startsWith("file://")) Url(path).encodedPath.decodeURLPart() else path)
            if (action == "writeText") { put("content", content); put("overwrite", overwrite) }
        })
        return DFile(
            name = row.getValue("name").jsonPrimitive.content,
            path = row.getValue("path").jsonPrimitive.content,
            permission = row.getValue("permission").jsonPrimitive.content,
            createdAt = Instant.fromEpochMilliseconds(row.getValue("createdAt").jsonPrimitive.long),
            updatedAt = Instant.fromEpochMilliseconds(row.getValue("updatedAt").jsonPrimitive.long),
            size = row.getValue("size").jsonPrimitive.long,
            isDir = row.getValue("isDir").jsonPrimitive.boolean,
            childCount = row.getValue("childCount").jsonPrimitive.int,
        )
    }
}
