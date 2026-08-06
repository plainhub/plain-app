package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import kotlinx.serialization.Serializable

@GraphQLType
@Serializable
data class ImageSearchStatus(
    val status: String,
    val downloadProgress: Int,
    val errorMessage: String,
    val modelSize: Long,
    val modelDir: String,
    val isIndexing: Boolean,
    val totalImages: Int,
    val indexedImages: Int,
)
