package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.db.DSession
import com.ismartcoding.plain.web.HttpServerManager
import com.ismartcoding.plain.web.SessionList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

data class VSession(val clientId: String, val clientIP: String, val osName: String, val osVersion: String, val browserName: String, val browserVersion: String, val createdAt: Instant, val updatedAt: Instant) {
    companion object {
        fun from(data: DSession): VSession {
            return VSession(
                data.clientId,
                data.clientIP,
                data.osName,
                data.osVersion,
                data.browserName,
                data.browserVersion,
                data.createdAt,
                data.updatedAt,
            )
        }
    }
}

class SessionsViewModel : ViewModel() {
    private val _itemsFlow = MutableStateFlow(mutableStateListOf<VSession>())
    val itemsFlow: StateFlow<List<VSession>> get() = _itemsFlow

    fun fetch() {
        viewModelScope.launch(Dispatchers.IO) {
            _itemsFlow.value = SessionList.getItemsAsync().map { VSession.from(it) }.toMutableStateList()
        }
    }

    fun delete(clientId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            SessionList.deleteAsync(clientId)
            _itemsFlow.value.removeIf { it.clientId == clientId }
            HttpServerManager.loadTokenCache()
        }
    }
}
