package com.ismartcoding.plain.platform

import com.ismartcoding.plain.db.DSession
import com.ismartcoding.plain.web.HttpServerManager
import com.ismartcoding.plain.web.SessionList

actual suspend fun fetchSessionsListItemsAsync(): List<DSession> = SessionList.getItemsAsync()

actual suspend fun deleteSessionListItemAsync(clientId: String) {
    SessionList.deleteAsync(clientId)
    HttpServerManager.loadTokenCache()
}

actual suspend fun createCustomSessionTokenAsync(name: String) {
    SessionList.createCustomTokenAsync(name)
    HttpServerManager.loadTokenCache()
}

actual suspend fun renameSessionListItemAsync(clientId: String, name: String): Boolean =
    SessionList.renameAsync(clientId, name)

actual suspend fun reloadSessionTokenCache() {
    HttpServerManager.loadTokenCache()
}
