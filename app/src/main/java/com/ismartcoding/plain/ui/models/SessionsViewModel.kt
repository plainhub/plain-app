package com.ismartcoding.plain.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.db.DSession
import com.ismartcoding.plain.web.HttpServerManager
import com.ismartcoding.plain.web.SessionList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SessionsViewModel : ViewModel() {
    private val _sessions = MutableStateFlow(listOf<DSession>())
    val sessions: StateFlow<List<DSession>> get() = _sessions.asStateFlow()

    fun fetch() {
        viewModelScope.launch(Dispatchers.IO) {
            _sessions.value = SessionList.getItems()
        }
    }

    fun delete(session: DSession) {
        viewModelScope.launch(Dispatchers.IO) {
            SessionList.deleteAsync(session)
            _sessions.value = SessionList.getItems()
            HttpServerManager.loadTokenCache()
        }
    }
}