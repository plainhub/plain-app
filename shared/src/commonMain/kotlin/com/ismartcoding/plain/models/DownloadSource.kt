package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("__typename")
sealed interface DownloadSource

@Serializable
@SerialName("ResourceDownloadSource")
data class ResourceDownloadSource(val urls: List<String>) : DownloadSource

@Serializable
@SerialName("ChatDownloadSource")
data class ChatDownloadSource(val messageId: String, val peerId: String, val fileIds: List<String>) : DownloadSource
