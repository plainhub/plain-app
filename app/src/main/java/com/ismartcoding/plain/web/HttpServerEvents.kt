package com.ismartcoding.plain.web

import com.ismartcoding.plain.db.DChat

class HttpServerEvents {
    class MessageCreatedEvent(val items: List<DChat>)

    class MessageUpdatedEvent(val id: String)
}
