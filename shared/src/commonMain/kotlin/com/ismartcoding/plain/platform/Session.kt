package com.ismartcoding.plain.platform

import com.ismartcoding.plain.db.DSession
import com.ismartcoding.plain.helpers.Base64Lenient
import com.ismartcoding.plain.httpserver.HttpServerManager
import com.ismartcoding.plain.httpserver.SessionList

suspend fun fetchSessionsListItemsAsync(): List<DSession> = SessionList.getItemsAsync()

suspend fun deleteSessionListItemAsync(clientId: String) {
    SessionList.deleteAsync(clientId)
    HttpServerManager.tokenCache.invalidate(clientId)
    HttpServerManager.clientIpCache.invalidate(clientId)
}

suspend fun createCustomSessionTokenAsync(name: String) {
    val item = SessionList.createCustomTokenAsync(name)
    HttpServerManager.tokenCache.put(item.clientId, Base64Lenient.decode(item.token))
}

suspend fun renameSessionListItemAsync(clientId: String, name: String): Boolean =
    SessionList.renameAsync(clientId, name)
