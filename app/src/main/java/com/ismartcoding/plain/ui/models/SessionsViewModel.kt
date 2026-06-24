package com.ismartcoding.plain.ui.models

import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.db.DSession
import com.ismartcoding.plain.web.HttpServerManager
import com.ismartcoding.plain.web.SessionList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Instant

data class VSession(
    val clientId: String,
    val name: String,
    val type: String,
    val token: String,
    val clientIP: String,
    val osName: String,
    val osVersion: String,
    val browserName: String,
    val browserVersion: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastActiveAt: Instant?,
) {
    val isCustom: Boolean
        get() = type == DSession.TYPE_CUSTOM

    companion object {
        fun from(data: DSession): VSession {
            return VSession(
                data.clientId,
                data.name,
                data.type,
                data.token,
                data.clientIP,
                data.osName,
                data.osVersion,
                data.browserName,
                data.browserVersion,
                data.createdAt,
                data.updatedAt,
                data.lastActiveAt,
            )
        }
    }
}

class SessionsViewModel : ViewModel() {
    private val _itemsFlow = MutableStateFlow<List<VSession>>(emptyList())
    val itemsFlow: StateFlow<List<VSession>> = _itemsFlow

    fun fetch() {
        launchSafe {
            _itemsFlow.value = SessionList.getItemsAsync().map { VSession.from(it) }
        }
    }

    fun delete(clientId: String) {
        launchSafe {
            SessionList.deleteAsync(clientId)
            _itemsFlow.value = _itemsFlow.value.filter { it.clientId != clientId }
            HttpServerManager.loadTokenCache()
        }
    }

    fun createCustomToken(name: String) {
        launchSafe {
            SessionList.createCustomTokenAsync(name)
            _itemsFlow.value = SessionList.getItemsAsync().map { VSession.from(it) }
            HttpServerManager.loadTokenCache()
        }
    }

    fun rename(clientId: String, name: String) {
        launchSafe {
            val changed = SessionList.renameAsync(clientId, name)
            if (changed) {
                _itemsFlow.value = SessionList.getItemsAsync().map { VSession.from(it) }
            }
        }
    }
}
