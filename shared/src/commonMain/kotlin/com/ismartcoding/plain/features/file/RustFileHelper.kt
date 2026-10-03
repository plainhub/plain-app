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

    suspend fun list(root: String, showHidden: Boolean, sortBy: FileSortBy): List<DFile> = read(root, "", null, showHidden, sortBy)["items"]!!.jsonArray.map { decode(it.jsonObject) }
    suspend fun searchName(text: String, root: String, showHidden: Boolean, sortBy: FileSortBy): List<DFile> = read(root, "", text, showHidden, sortBy)["items"]!!.jsonArray.map { decode(it.jsonObject) }
    suspend fun search(query: String, root: String, sortBy: FileSortBy, offset: Int = 0, limit: Int? = null): List<DFile> = read(root, query, null, null, sortBy, offset, limit)["items"]!!.jsonArray.map { decode(it.jsonObject) }
    suspend fun count(query: String, root: String): Int = read(root, query, null, null, FileSortBy.NAME_ASC, countOnly = true).getValue("count").jsonPrimitive.int

    suspend fun rename(path: String, name: String): String? {
        val value = RustContentApi.postJson("files/mutate", buildJsonObject { put("action", "rename"); put("path", path); put("name", name) }, longRunning = true).getValue("path")
        return if (value is JsonNull) null else value.jsonPrimitive.content
    }
    suspend fun delete(path: String): Boolean = RustContentApi.postJson("files/mutate", buildJsonObject { put("action", "delete"); put("path", path) }, longRunning = true).getValue("removed").jsonPrimitive.boolean

    suspend fun stat(path: String): DFile? {
        val row = RustContentApi.postJson("files/stat", buildJsonObject { put("path", path) }).getValue("file")
        return if (row is JsonNull) null else decode(row.jsonObject)
    }

    private suspend fun read(root: String, query: String, text: String?, showHidden: Boolean?, sortBy: FileSortBy, offset: Int = 0, limit: Int? = null, countOnly: Boolean = false): JsonObject = RustContentApi.postJson("files/read", buildJsonObject {
        put("root", root)
        put("query", query)
        put("text", text?.let(::JsonPrimitive) ?: JsonNull)
        put("showHidden", showHidden?.let(::JsonPrimitive) ?: JsonNull)
        put("sortBy", sortBy.name)
        put("offset", offset)
        put("limit", limit?.let(::JsonPrimitive) ?: JsonNull)
        put("countOnly", countOnly)
    }, longRunning = true)

    private suspend fun write(action: String, path: String, content: String = "", overwrite: Boolean = false): DFile {
        val row = RustContentApi.postJson("files/write", buildJsonObject {
            put("action", action)
            put("path", if (path.startsWith("file://")) Url(path).encodedPath.decodeURLPart() else path)
            if (action == "writeText") { put("content", content); put("overwrite", overwrite) }
        })
        return decode(row)
    }

    private fun decode(row: JsonObject): DFile = DFile(
            name = row.getValue("name").jsonPrimitive.content,
            path = row.getValue("path").jsonPrimitive.content,
            permission = row.getValue("permission").jsonPrimitive.content,
            createdAt = row["createdAt"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.long?.let(Instant::fromEpochMilliseconds),
            updatedAt = Instant.fromEpochMilliseconds(row.getValue("updatedAt").jsonPrimitive.long),
            size = row.getValue("size").jsonPrimitive.long,
            isDir = row.getValue("isDir").jsonPrimitive.boolean,
            childCount = row.getValue("childCount").jsonPrimitive.int,
            mediaId = row["mediaId"]?.jsonPrimitive?.content.orEmpty(),
    )
}
