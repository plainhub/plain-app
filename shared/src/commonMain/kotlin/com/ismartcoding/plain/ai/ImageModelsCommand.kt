package com.ismartcoding.plain.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
@kotlinx.serialization.json.JsonClassDiscriminator("action")
@Serializable
internal sealed class ImageModelsCommand {
    @Serializable @SerialName("search") data class Search(val text: String, val limit: Int) : ImageModelsCommand()
    @Serializable @SerialName("snapshot") data object Snapshot : ImageModelsCommand()
    @Serializable @SerialName("restore") data object Restore : ImageModelsCommand()
    @Serializable @SerialName("enable") data object Enable : ImageModelsCommand()
    @Serializable @SerialName("disable") data object Disable : ImageModelsCommand()
    @Serializable @SerialName("release") data object Release : ImageModelsCommand()
    @Serializable @SerialName("cancel") data object Cancel : ImageModelsCommand()
    @Serializable @SerialName("error") data class Error(val message: String) : ImageModelsCommand()
}
