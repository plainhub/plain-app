package com.ismartcoding.plain.ui.page.dlna

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.features.dlna.DlnaRulesSnapshot
import com.ismartcoding.plain.features.dlna.RustDlnaRules
import com.ismartcoding.plain.ui.models.launchSafe
import kotlinx.coroutines.flow.MutableStateFlow

class DlnaCastRulesViewModel : ViewModel() {
    val allowedFlow = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val deniedFlow = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    fun load() { viewModelScope.launchSafe { apply(RustDlnaRules.snapshot()) } }
    fun removeAllowed(ip: String) { viewModelScope.launchSafe { apply(RustDlnaRules.remove(ip, true)) } }
    fun removeDenied(ip: String) { viewModelScope.launchSafe { apply(RustDlnaRules.remove(ip, false)) } }
    private fun apply(state: DlnaRulesSnapshot) {
        allowedFlow.value = state.allowed.map { it.ip to it.name }
        deniedFlow.value = state.denied.map { it.ip to it.name }
    }
}
