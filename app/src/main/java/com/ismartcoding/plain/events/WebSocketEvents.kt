package com.ismartcoding.plain.events

import com.ismartcoding.lib.channel.ChannelEvent

// The events sent to the web client via WebSocket
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
    NOTIFICATION_CREATED(7),
    NOTIFICATION_UPDATED(8),
    NOTIFICATION_DELETED(9),
    NOTIFICATION_REFRESHED(10),
}
