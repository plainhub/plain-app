package com.ismartcoding.plain.ui.sessions

import com.ismartcoding.plain.ui.state.launchSafe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.features.sessions.CreatedSession
import com.ismartcoding.plain.features.sessions.RustSessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SessionsViewModel : ViewModel() {
    private val _itemsFlow = MutableStateFlow<List<VSession>>(emptyList())
    val itemsFlow: StateFlow<List<VSession>> = _itemsFlow

    fun fetch() {
        viewModelScope.launchSafe {
            _itemsFlow.value = RustSessionStore.list()
        }
    }

    fun delete(clientId: String) {
        viewModelScope.launchSafe {
            RustSessionStore.delete(clientId)
            _itemsFlow.value = _itemsFlow.value.filter { it.clientId != clientId }
        }
    }

    fun createCustomToken(name: String, onCreated: (CreatedSession) -> Unit) {
        viewModelScope.launchSafe {
            val created = RustSessionStore.create(name)
            onCreated(created)
            _itemsFlow.value = RustSessionStore.list()
        }
    }

    fun rename(clientId: String, name: String) {
        viewModelScope.launchSafe {
            val changed = RustSessionStore.rename(clientId, name)
            if (changed) {
                _itemsFlow.value = RustSessionStore.list()
            }
        }
    }
}
