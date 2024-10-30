package com.ismartcoding.plain.web

import com.ismartcoding.lib.channel.ChannelEvent
import com.ismartcoding.plain.db.DChat

class HttpServerEvents {
    class MessageCreatedEvent(val items: List<DChat>) : ChannelEvent()

    class MessageUpdatedEvent(val id: String) : ChannelEvent()
}
