package com.ismartcoding.plain.events

import com.ismartcoding.lib.channel.ChannelEvent
import com.ismartcoding.plain.ui.page.pomodoro.PomodoroState
import kotlinx.serialization.Serializable

sealed class WebSocketData {
    data class Text(val value: String) : WebSocketData()
    data class Binary(val value: ByteArray) : WebSocketData() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Binary

            return value.contentEquals(other.value)
        }

        override fun hashCode(): Int {
            return value.contentHashCode()
        }
    }
}

// The events sent to the web client via WebSocket
class WebSocketEvent(
    val type: EventType,
    val data: WebSocketData,
) : ChannelEvent() // Event will be sent to web client
{
    constructor(type: EventType, data: String) : this(type, WebSocketData.Text(data))
    constructor(type: EventType, data: ByteArray) : this(type, WebSocketData.Binary(data))
}

enum class EventType(val value: Int) {
    MESSAGE_CREATED(1),
    MESSAGE_DELETED(2),
    MESSAGE_UPDATED(3),
    FEEDS_FETCHED(4),
    SCREEN_MIRRORING(5),
    WEBRTC_SIGNALING(6),
    NOTIFICATION_CREATED(7),
    NOTIFICATION_UPDATED(8),
    NOTIFICATION_DELETED(9),
    NOTIFICATION_REFRESHED(10),
    POMODORO_ACTION(11),
    POMODORO_SETTINGS_UPDATE(12),
    CHAT_SETTINGS_UPDATE(13),
}


@Serializable
data class PomodoroActionData(
    val action: String, val timeLeft: Int,
    val totalTime: Int, val completedCount: Int,
    val round: Int, val state: PomodoroState
) // action: "start", "pause",  "stop"