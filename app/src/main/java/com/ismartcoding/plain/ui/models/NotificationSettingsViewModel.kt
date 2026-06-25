package com.ismartcoding.plain.ui.models
import com.ismartcoding.plain.preferences.*

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.data.DPackage
import com.ismartcoding.plain.data.NotificationFilterData
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.features.PackageHelper
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.preferences.NotificationFilterPreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NotificationSettingsViewModel : ViewModel() {
    private val _selectedAppsFlow = MutableStateFlow<List<DPackage>>(emptyList())
    val selectedAppsFlow: StateFlow<List<DPackage>> = _selectedAppsFlow
    
    private val _allAppsFlow = MutableStateFlow<List<DPackage>>(emptyList())
    val allAppsFlow: StateFlow<List<DPackage>> = _allAppsFlow
    
    var filterData = mutableStateOf(NotificationFilterData())
    var isLoading = mutableStateOf(true)
    var showAppSelector = mutableStateOf(false)
    var appsLoaded = mutableStateOf(false)
    var searchQuery = mutableStateOf("")
    
    // For multiple selection in app selector
    val selectedAppIds = mutableStateListOf<String>()

    suspend fun loadDataAsync(context: Context) {
        try {
            filterData.value = NotificationFilterPreference.getValueAsync()
            val apps = mutableListOf<DPackage>()
            withIO {
                filterData.value.apps.forEach { packageName ->
                    try {
                        val app = PackageHelper.getPackage(packageName)
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

    suspend fun loadAllAppsAsync(context: Context) = withIO {
        if (appsLoaded.value) return@withIO

        try {
            val apps = PackageHelper.searchAsync("", Int.MAX_VALUE, 0, FileSortBy.NAME_ASC)
                .filter { !filterData.value.apps.contains(it.id) && it.id != context.packageName }
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

    suspend fun toggleModeAsync(context: Context) {
        val newMode = if (filterData.value.mode == "allowlist") "blacklist" else "allowlist"
        NotificationFilterPreference.setModeAsync(newMode)
        filterData.value = filterData.value.copy(mode = newMode)
        refreshNotifications()
    }

    suspend fun removeAppAsync(context: Context, packageName: String) {
        NotificationFilterPreference.toggleAppAsync(packageName)
        filterData.value = NotificationFilterPreference.getValueAsync()
        loadSelectedApps(context)
        refreshNotifications()
    }

    suspend fun addAppsAsync(context: Context, packageNames: List<String>) {
        packageNames.forEach { packageName ->
            NotificationFilterPreference.toggleAppAsync(packageName)
        }
        filterData.value = NotificationFilterPreference.getValueAsync()
        loadSelectedApps(context)
        refreshNotifications()
    }

    suspend fun clearAllAsync(context: Context) {
        NotificationFilterPreference.putAsync(filterData.value.copy(apps = emptySet()))
        filterData.value = NotificationFilterPreference.getValueAsync()
        _selectedAppsFlow.value = emptyList()
        refreshNotifications()
    }

    private suspend fun loadSelectedApps(context: Context) = withIO {
        val apps = mutableListOf<DPackage>()
        filterData.value.apps.forEach { packageName ->
            try {
                val app = PackageHelper.getPackage(packageName)
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