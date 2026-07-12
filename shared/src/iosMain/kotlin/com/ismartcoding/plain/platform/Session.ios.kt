package com.ismartcoding.plain.platform

import com.ismartcoding.plain.db.DSession

actual suspend fun fetchSessionsListItemsAsync(): List<DSession> = emptyList()

actual suspend fun deleteSessionListItemAsync(clientId: String) {
}

actual suspend fun createCustomSessionTokenAsync(name: String) {
}

actual suspend fun renameSessionListItemAsync(clientId: String, name: String): Boolean = false

actual suspend fun reloadSessionTokenCache() {
}
