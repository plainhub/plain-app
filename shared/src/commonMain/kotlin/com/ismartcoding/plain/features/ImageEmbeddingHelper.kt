package com.ismartcoding.plain.features

import com.ismartcoding.plain.ai.SemanticSearchResult
import com.ismartcoding.plain.api.*
import com.ismartcoding.plain.db.DImageEmbedding
import kotlin.io.encoding.Base64
import kotlinx.serialization.json.*

object ImageEmbeddingHelper {
    suspend fun getAllIds(): List<String> = RustContentApi.query("imageHostEmbeddingIds").getValue("imageHostEmbeddingIds").jsonArray.map { it.jsonPrimitive.content }
    suspend fun count(): Long = RustContentApi.query("imageHostEmbeddingCount").getValue("imageHostEmbeddingCount").jsonPrimitive.long
    suspend fun insertAll(items: List<DImageEmbedding>) {
        val input = items.joinToString(",", "[", "]") { "{ id: ${gql(it.id)}, path: ${gql(it.path)}, embeddingBase64: ${gql(Base64.encode(it.embedding))} }" }
        RustContentApi.mutate("imageHostSaveEmbeddings(items: $input)")
    }
    suspend fun deleteByIds(ids: List<String>) { RustContentApi.mutate("imageHostRemoveEmbeddings(ids: ${gqlIds(ids)})") }
    suspend fun deleteAll() { RustContentApi.mutate("imageHostClearEmbeddings") }
    suspend fun search(embedding: ByteArray, limit: Int): List<SemanticSearchResult> = RustContentApi.query("imageHostSearch(embeddingBase64: ${gql(Base64.encode(embedding))}, maxResults: $limit) { imageId score }")
        .getValue("imageHostSearch").jsonArray.map { value -> value.jsonObject.let { SemanticSearchResult(it.string("imageId"), it.getValue("score").jsonPrimitive.float) } }
}
