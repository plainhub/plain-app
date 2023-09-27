package com.ismartcoding.plain.web.websocket

import io.ktor.server.websocket.*

data class WebSocketSession(val id: Long, val clientId: String, val session: DefaultWebSocketServerSession)
