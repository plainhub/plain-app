package com.ismartcoding.plain.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
@kotlinx.serialization.json.JsonClassDiscriminator("action")
@Serializable
internal sealed class ImageSearchCommand {
    @Serializable @SerialName("rows") data class Rows(val text: String, val query: String, val offset: Int, val limit: Int, val sortBy: String) : ImageSearchCommand()
    @Serializable @SerialName("count") data class Count(val text: String, val query: String) : ImageSearchCommand()
}
