package com.ismartcoding.plain.events

import com.ismartcoding.lib.channel.ChannelEvent
import com.ismartcoding.plain.db.DChat

// The events sent from the HTTP API
class HttpApiEvents {
    class MessageCreatedEvent(val items: List<DChat>) : ChannelEvent()

    class MessageUpdatedEvent(val id: String) : ChannelEvent()
}