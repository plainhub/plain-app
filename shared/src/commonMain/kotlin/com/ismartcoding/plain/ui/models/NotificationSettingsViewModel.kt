package com.ismartcoding.plain.ui.models
import com.ismartcoding.plain.preferences.*

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.data.NotificationFilterData
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.platform.DNotificationApp
import com.ismartcoding.plain.platform.getAllNotificationApps
import com.ismartcoding.plain.platform.getNotificationApp
import com.ismartcoding.plain.preferences.NotificationFilterPreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NotificationSettingsViewModel : ViewModel() {
    private val _selectedAppsFlow = MutableStateFlow<List<DNotificationApp>>(emptyList())
    val selectedAppsFlow: StateFlow<List<DNotificationApp>> = _selectedAppsFlow

    private val _allAppsFlow = MutableStateFlow<List<DNotificationApp>>(emptyList())
    val allAppsFlow: StateFlow<List<DNotificationApp>> = _allAppsFlow

    var filterData = mutableStateOf(NotificationFilterData())
    var isLoading = mutableStateOf(true)
    var showAppSelector = mutableStateOf(false)
    var appsLoaded = mutableStateOf(false)
    var searchQuery = mutableStateOf("")

    // For multiple selection in app selector
    val selectedAppIds = mutableStateListOf<String>()

    suspend fun loadDataAsync() {
        try {
            filterData.value = NotificationFilterPreference.getValueAsync()
            val apps = mutableListOf<DNotificationApp>()
            withIO {
                filterData.value.apps.forEach { packageName ->
                    try {
                        val app = getNotificationApp(packageName) ?: throw IllegalStateException("not installed")
                        apps.add(app)
                    } catch (e: Exception) {
                        // App might be uninstalled, remove from list
                        NotificationFilterPreference.toggleAppAsync(packageName)
                    }
                }
            }
            _selectedAppsFlow.value = apps.sortedBy { it.name }
            isLoading.value = false
        } catch (e: Exception) {
            isLoading.value = false
        }
    }

    suspend fun loadAllAppsAsync() = withIO {
        if (appsLoaded.value) return@withIO

        try {
            val apps = getAllNotificationApps()
                .filter { !filterData.value.apps.contains(it.id) }
            _allAppsFlow.value = apps
            appsLoaded.value = true
        } catch (e: Exception) {
            appsLoaded.value = false
        }
    }

    fun refreshNotifications() {
        sendEvent(
            WebSocketEvent(
                EventType.NOTIFICATION_REFRESHED, ""
            )
        )
    }

    suspend fun toggleModeAsync() {
        val newMode = if (filterData.value.mode == "allowlist") "blacklist" else "allowlist"
        NotificationFilterPreference.setModeAsync(newMode)
        filterData.value = filterData.value.copy(mode = newMode)
        refreshNotifications()
    }

    suspend fun removeAppAsync(packageName: String) {
        NotificationFilterPreference.toggleAppAsync(packageName)
        filterData.value = NotificationFilterPreference.getValueAsync()
        loadSelectedApps()
        refreshNotifications()
    }

    suspend fun addAppsAsync(packageNames: List<String>) {
        packageNames.forEach { packageName ->
            NotificationFilterPreference.toggleAppAsync(packageName)
        }
        filterData.value = NotificationFilterPreference.getValueAsync()
        loadSelectedApps()
        refreshNotifications()
    }

    suspend fun clearAllAsync() {
        NotificationFilterPreference.putAsync(filterData.value.copy(apps = emptySet()))
        filterData.value = NotificationFilterPreference.getValueAsync()
        _selectedAppsFlow.value = emptyList()
        refreshNotifications()
    }

    private suspend fun loadSelectedApps() = withIO {
        val apps = mutableListOf<DNotificationApp>()
        filterData.value.apps.forEach { packageName ->
            try {
                val app = getNotificationApp(packageName) ?: throw IllegalStateException("not installed")
                apps.add(app)
            } catch (e: Exception) {
                // App might be uninstalled, remove from list
                NotificationFilterPreference.toggleAppAsync(packageName)
            }
        }
        _selectedAppsFlow.value = apps.sortedBy { it.name }
    }

    fun clearSelectedApps() {
        selectedAppIds.clear()
    }

    fun toggleAppSelection(packageName: String) {
        if (selectedAppIds.contains(packageName)) {
            selectedAppIds.remove(packageName)
        } else {
            selectedAppIds.add(packageName)
        }
    }

    fun showAppSelectorDialog() {
        showAppSelector.value = true
    }
}
