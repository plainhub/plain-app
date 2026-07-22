package com.ismartcoding.plain.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.platform.createCustomSessionTokenAsync
import com.ismartcoding.plain.platform.deleteSessionListItemAsync
import com.ismartcoding.plain.platform.fetchSessionsListItemsAsync
import com.ismartcoding.plain.platform.renameSessionListItemAsync
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SessionsViewModel : ViewModel() {
    private val _itemsFlow = MutableStateFlow<List<VSession>>(emptyList())
    val itemsFlow: StateFlow<List<VSession>> = _itemsFlow

    fun fetch() {
        viewModelScope.launchSafe {
            _itemsFlow.value = fetchSessionsListItemsAsync().map { VSession.from(it) }
        }
    }

    fun delete(clientId: String) {
        viewModelScope.launchSafe {
            deleteSessionListItemAsync(clientId)
            _itemsFlow.value = _itemsFlow.value.filter { it.clientId != clientId }
        }
    }

    fun createCustomToken(name: String) {
        viewModelScope.launchSafe {
            createCustomSessionTokenAsync(name)
            _itemsFlow.value = fetchSessionsListItemsAsync().map { VSession.from(it) }
        }
    }

    fun rename(clientId: String, name: String) {
        viewModelScope.launchSafe {
            val changed = renameSessionListItemAsync(clientId, name)
            if (changed) {
                _itemsFlow.value = fetchSessionsListItemsAsync().map { VSession.from(it) }
            }
        }
    }
}
