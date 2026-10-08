package com.ismartcoding.plain.api

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.events.WebSocketEvent

object WebSocketHelper {
    suspend fun sendEventAsync(event: WebSocketEvent): Int = RustContentApi.publish(event)
}
