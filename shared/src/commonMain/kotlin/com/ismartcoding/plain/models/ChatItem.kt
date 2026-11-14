package com.ismartcoding.plain.models

import com.ismartcoding.plain.features.chat.ChatStatus
import com.ismartcoding.plain.features.chat.models.MessageContent
import com.ismartcoding.plain.features.chat.models.MessageDeliveryResult
import com.ismartcoding.plain.features.chat.models.MessageStatusData
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlin.time.Instant
import com.ismartcoding.plain.lib.TimeHelper

@Serializable
data class ChatItem(
    @Required var fromId: String = "",
    @Required var toId: String = "",
    var channelId: String? = null,
    @Required var id: String = "",
    var content: ChatContent,
    @Required var createdAt: Instant = TimeHelper.now(),
    @Required var updatedAt: Instant = TimeHelper.now(),
    @Required var status: ChatStatus = ChatStatus.PENDING,
    @Required var deliveryResults: List<MessageDeliveryResult> = emptyList(),
) {
    val messageContent: MessageContent get() = content.message()
    fun deliveryStatus(): MessageStatusData? = deliveryResults.takeIf { it.isNotEmpty() }?.let(::MessageStatusData)
}
