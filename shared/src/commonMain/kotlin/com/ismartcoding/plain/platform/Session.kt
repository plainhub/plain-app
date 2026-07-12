package com.ismartcoding.plain.platform

import com.ismartcoding.plain.db.DSession

expect suspend fun fetchSessionsListItemsAsync(): List<DSession>

expect suspend fun deleteSessionListItemAsync(clientId: String)

expect suspend fun createCustomSessionTokenAsync(name: String)

expect suspend fun renameSessionListItemAsync(clientId: String, name: String): Boolean

expect suspend fun reloadSessionTokenCache()
