package com.ismartcoding.plain.web.websocket

import com.ismartcoding.lib.channel.ChannelEvent

class WebSocketEvent(
    val type: EventType,
    val data: Any, // String or ByteArray
): ChannelEvent() // Event will be sent to web client

enum class EventType(val value: Int) {
    MESSAGE_CREATED(1),
    MESSAGE_DELETED(2),
    MESSAGE_UPDATED(3),
    FEEDS_FETCHED(4),
    SCREEN_MIRRORING(5),
    AI_CHAT_REPLIED(6),
    NOTIFICATION_CREATED(7),
    NOTIFICATION_UPDATED(8),
    NOTIFICATION_DELETED(9),
}
