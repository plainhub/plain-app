package com.ismartcoding.plain.platform

import com.ismartcoding.plain.db.DSession
import com.ismartcoding.plain.httpserver.HttpServerManager
import com.ismartcoding.plain.httpserver.SessionList

suspend fun fetchSessionsListItemsAsync(): List<DSession> = SessionList.getItemsAsync()

suspend fun deleteSessionListItemAsync(clientId: String) {
    SessionList.deleteAsync(clientId)
    HttpServerManager.loadTokenCache()
}

suspend fun createCustomSessionTokenAsync(name: String) {
    SessionList.createCustomTokenAsync(name)
    HttpServerManager.loadTokenCache()
}

suspend fun renameSessionListItemAsync(clientId: String, name: String): Boolean =
    SessionList.renameAsync(clientId, name)

suspend fun reloadSessionTokenCache() {
    HttpServerManager.loadTokenCache()
}
