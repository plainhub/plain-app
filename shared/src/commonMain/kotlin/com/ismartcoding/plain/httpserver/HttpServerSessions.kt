package com.ismartcoding.plain.httpserver

import kotlinx.coroutines.flow.MutableStateFlow

val onlineClientIds = MutableStateFlow<Set<String>>(emptySet())

internal fun setOnlineClientIds(ids: Set<String>) {
    onlineClientIds.value = ids
}

/**
 * Close all live WebSocket sessions and clear the online-client set. Shared by
 * the `/shutdown` route and the in-process stop orchestrator so stopping never
 * needs an HTTP round-trip through the server being torn down.
 */
suspend fun closeAllWsSessions() {
    HttpServerManager.wsSessions.toList().forEach { it.close() }
    HttpServerManager.wsSessions.clear()
    onlineClientIds.value = emptySet()
}
