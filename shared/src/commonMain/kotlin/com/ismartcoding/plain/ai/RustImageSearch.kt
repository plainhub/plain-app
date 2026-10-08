package com.ismartcoding.plain.ai

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.data.DImage
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.features.system.MediaFacts
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.json.*
import kotlin.time.Instant

internal object RustImageSearch {
    private suspend fun call(command: ImageSearchCommand): JsonObject = RustContentApi.postJsonOrThrow("system/image-search", JsonHelper.jsonEncodeToElement<ImageSearchCommand>(command).jsonObject)
    suspend fun rows(text: String, query: String, limit: Int, offset: Int, sortBy: FileSortBy): List<DImage> {
        val response = call(ImageSearchCommand.Rows(text,query,offset,limit,sortBy.name))
        return JsonHelper.jsonDecodeFromElement<List<MediaFacts>>(response.getValue("items")).map { row ->
            DImage(row.id,row.title,row.path,row.size,row.width,row.height,row.rotation,row.bucketId,
                Instant.parse(row.createdAt),Instant.parse(row.updatedAt),row.takenAt?.let(Instant::parse),row.isFavorite)
        }
    }
    suspend fun count(text: String, query: String): Int = call(ImageSearchCommand.Count(text,query)).getValue("count").jsonPrimitive.int
}
