package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.db.*
import com.ismartcoding.plain.enums.ChatStatus
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLField
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
    @GraphQLField(description = "Message envelope JSON: {type: TEXT|IMAGES|FILES|SHARE, value: {...}} — parse `value` according to `type`; the typed payload is also mirrored in `data`.")
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    @Transient private val _content: DMessageContent? = null,
    @GraphQLIgnore @Contextual var data: ChatItemContent? = null,
    val status: ChatStatus = ChatStatus.SENT,
    @GraphQLField(description = "Per-recipient delivery details as JSON (peer delivery results); empty when the message has no delivery failures. Drives SENT/PARTIAL/FAILED alongside `status`.")
    val statusData: String = "",
) {
    fun getContentData(): ChatItemContent? {
        return when (_content?.value) {
            is DMessageImages -> {
                ChatItemContent.ChatImages((_content.value as DMessageImages).items.map {
                    val json = buildJsonObject {
                        put("path", it.uri)
                        put("name", it.fileName)
                    }
                    getFileId(json.toString())
                })
            }

            is DMessageFiles -> {
                ChatItemContent.ChatFiles((_content.value as DMessageFiles).items.map {
                    val json = buildJsonObject {
                        put("path", it.uri)
                        put("name", it.fileName)
                    }
                    getFileId(json.toString())
                })
            }

            is DMessageText -> {
                val messageText = _content.value as DMessageText
                val imageIds = messageText.linkPreviews.mapNotNull {
                    val p = it.imageLocalPath
                    if (p.isNullOrEmpty()) null else getFileId(p)
                }
                ChatItemContent.ChatText(imageIds)
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
    /** Shared image fileId list (app file store ids, String). */
    data class ChatImages(val ids: List<String>) : ChatItemContent()

    @GraphQLType
    @Serializable
    /** Shared file fileId list (app file store ids, String). */
    data class ChatFiles(val ids: List<String>) : ChatItemContent()

    @GraphQLType
    @Serializable
    /** fileId list (String) of locally cached link-preview images; the message text lives inside the ChatItem.content envelope (value.text). */
    data class ChatText(val linkPreviewImageIds: List<String>) : ChatItemContent()
}

fun DChat.toModel(): ChatItem {
    val ci = ChatItem(ID(id), fromId, toId, channelId, content.toJSONString(), createdAt, updatedAt, content, status = status, statusData = statusData)
    ci.data = ci.getContentData()
    return ci
}

fun dchatToModel(c: DChat): ChatItem = c.toModel()
