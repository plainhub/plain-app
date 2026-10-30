package com.ismartcoding.plain.features.chat

import com.ismartcoding.plain.features.chat.data.ChatTarget
import com.ismartcoding.plain.features.chat.models.MessageContent
import com.ismartcoding.plain.features.chat.models.toJSONString
import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.models.ChatItem
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import kotlin.time.Instant

object ShareSendClient : ShareSendApi()

open class ShareSendApi internal constructor(private val api: com.ismartcoding.plain.rust.graphql.GraphqlClient = GraphqlApi) {
    suspend fun sendContent(targets: List<ChatTarget>, content: MessageContent): List<ChatItem> =
        api.execute(Mutation.sendChatItems(targets.map { it.encodedToId }, content.toJSONString()))

    suspend fun sendShare(targets: List<ChatTarget>, shareId: String): List<ChatItem> =
        api.execute(Mutation.sendShare(targets.map { it.encodedToId }, shareId))

    suspend fun sendFolderShare(
        targets: List<ChatTarget>,
        dirPath: String,
        name: String,
        expiresAt: Instant?,
    ): List<ChatItem> =
        api.execute(Mutation.sendFolderShare(targets.map { it.encodedToId }, dirPath, name, expiresAt))
}
