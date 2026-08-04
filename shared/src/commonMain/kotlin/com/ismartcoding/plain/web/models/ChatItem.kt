package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.db.*
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLIgnore
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLUnion
import com.ismartcoding.plain.helpers.getFileId
import kotlin.time.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@GraphQLType
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
    @GraphQLIgnore @Contextual var data: ChatItemContent? = null,
    val status: String = "",
    val statusData: String = "",
) {
    fun getContentData(): ChatItemContent? {
        return when (_content?.value) {
            is DMessageImages -> {
                ChatItemContent.MessageImages((_content.value as DMessageImages).items.map {
                    val json = buildJsonObject {
                        put("path", it.uri)
                        put("name", it.fileName)
                    }
                    getFileId(json.toString())
                })
            }

            is DMessageFiles -> {
                ChatItemContent.MessageFiles((_content.value as DMessageFiles).items.map {
                    val json = buildJsonObject {
                        put("path", it.uri)
                        put("name", it.fileName)
                    }
                    getFileId(json.toString())
                })
            }

            is DMessageText -> {
                val messageText = _content.value as DMessageText
                val imageIds = messageText.linkPreviews
                    .map { val p = it.imageLocalPath; if (p.isNullOrEmpty()) "" else getFileId(p) }
                ChatItemContent.MessageText(imageIds)
            }

            else -> {
                null
            }
        }
    }
}

@GraphQLUnion
@Serializable
@Polymorphic
sealed class ChatItemContent {
    @GraphQLType
    @Serializable
    data class MessageImages(val ids: List<String>) : ChatItemContent()

    @GraphQLType
    @Serializable
    data class MessageFiles(val ids: List<String>) : ChatItemContent()

    @GraphQLType
    @Serializable
    data class MessageText(val ids: List<String>) : ChatItemContent()
}

fun DChat.toModel(): ChatItem {
    val ci = ChatItem(ID(id), fromId, toId, channelId, content.toJSONString(), createdAt, updatedAt, content, status = status, statusData = statusData)
    ci.data = ci.getContentData()
    return ci
}

fun dchatToModel(c: DChat): ChatItem = c.toModel()
