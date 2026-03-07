package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.db.*
import com.ismartcoding.plain.helpers.FileHelper
import kotlin.time.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.json.JSONObject

@Serializable
data class ChatItem(
    val id: ID,
    val fromId: String,
    val toId: String,
    val channelId: String,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    @Transient private val _content: DMessageContent? = null,
    @Contextual var data: ChatItemContent? = null,
) {
    fun getContentData(): ChatItemContent? {
        return when (_content?.value) {
            is DMessageImages -> {
                ChatItemContent.MessageImages((_content.value as DMessageImages).items.map {
                    val json = JSONObject()
                    json.put("path", it.uri)
                    json.put("name", it.fileName)
                    FileHelper.getFileId(json.toString())
                })
            }

            is DMessageFiles -> {
                ChatItemContent.MessageFiles((_content.value as DMessageFiles).items.map {
                    val json = JSONObject()
                    json.put("path", it.uri)
                    json.put("name", it.fileName)
                    FileHelper.getFileId(json.toString())
                })
            }

            is DMessageText -> {
                val messageText = _content.value as DMessageText
                val imageIds = messageText.linkPreviews
                    .map { if (it.imageLocalPath.isNullOrEmpty()) "" else FileHelper.getFileId(it.imageLocalPath) }
                ChatItemContent.MessageText(imageIds)
            }

            else -> {
                null
            }
        }
    }
}

@Serializable
@Polymorphic
sealed class ChatItemContent {
    @Serializable
    data class MessageImages(val ids: List<String>) : ChatItemContent()

    @Serializable
    data class MessageFiles(val ids: List<String>) : ChatItemContent()

    @Serializable
    data class MessageText(val ids: List<String>) : ChatItemContent()
}

fun DChat.toModel(): ChatItem {
    return ChatItem(ID(id), fromId, toId, channelId, content.toJSONString(), createdAt, updatedAt, content)
}
