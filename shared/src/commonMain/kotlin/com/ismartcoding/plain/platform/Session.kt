package com.ismartcoding.plain.platform

import com.ismartcoding.plain.ui.models.VSession
import com.ismartcoding.plain.features.session.RustSessionStore

suspend fun fetchSessionsListItemsAsync(): List<VSession> = RustSessionStore.list()

suspend fun deleteSessionListItemAsync(clientId: String) {
    RustSessionStore.delete(clientId)
}

suspend fun createCustomSessionTokenAsync(name: String) = RustSessionStore.create(name)
suspend fun renameSessionListItemAsync(clientId: String, name: String): Boolean = RustSessionStore.rename(clientId, name)
