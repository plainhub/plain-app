package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.plain.data.DPackage
import com.ismartcoding.plain.data.NotificationFilterData
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.features.PackageHelper
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.preference.NotificationFilterPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NotificationSettingsViewModel : ViewModel() {
    private val _selectedAppsFlow = MutableStateFlow(mutableStateListOf<DPackage>())
    val selectedAppsFlow: StateFlow<List<DPackage>> get() = _selectedAppsFlow
    
    private val _allAppsFlow = MutableStateFlow(mutableStateListOf<DPackage>())
    val allAppsFlow: StateFlow<List<DPackage>> get() = _allAppsFlow
    
    var filterData = mutableStateOf(NotificationFilterData())
    var isLoading = mutableStateOf(true)
    var showAppSelector = mutableStateOf(false)
    var appsLoaded = mutableStateOf(false)
    var searchQuery = mutableStateOf("")
    
    // For multiple selection in app selector
    val selectedAppIds = mutableStateListOf<String>()

    suspend fun loadDataAsync(context: Context) {
        try {
            filterData.value = NotificationFilterPreference.getValueAsync(context)
            val apps = mutableListOf<DPackage>()
            filterData.value.apps.forEach { packageName ->
                try {
                    val app = PackageHelper.getPackage(packageName)
                    apps.add(app)
                } catch (e: Exception) {
                    // App might be uninstalled, remove from list
                    NotificationFilterPreference.toggleAppAsync(context, packageName)
                }
            }
            _selectedAppsFlow.value = apps.sortedBy { it.name }.toMutableStateList()
            isLoading.value = false
        } catch (e: Exception) {
            isLoading.value = false
        }
    }

    suspend fun loadAllAppsAsync(context: Context) {
        if (appsLoaded.value) return
        
        try {
            val apps = PackageHelper.searchAsync("", Int.MAX_VALUE, 0, FileSortBy.NAME_ASC)
                .filter { !filterData.value.apps.contains(it.id) && it.id != context.packageName }
            _allAppsFlow.value = apps.toMutableStateList()
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
        NotificationFilterPreference.setModeAsync(context, newMode)
        filterData.value = filterData.value.copy(mode = newMode)
        refreshNotifications()
    }

    suspend fun removeAppAsync(context: Context, packageName: String) {
        NotificationFilterPreference.toggleAppAsync(context, packageName)
        filterData.value = NotificationFilterPreference.getValueAsync(context)
        loadSelectedApps(context)
        refreshNotifications()
    }

    suspend fun addAppsAsync(context: Context, packageNames: List<String>) {
        packageNames.forEach { packageName ->
            NotificationFilterPreference.toggleAppAsync(context, packageName)
        }
        filterData.value = NotificationFilterPreference.getValueAsync(context)
        loadSelectedApps(context)
        refreshNotifications()
    }

    suspend fun clearAllAsync(context: Context) {
        NotificationFilterPreference.putAsync(context, filterData.value.copy(apps = emptySet()))
        filterData.value = NotificationFilterPreference.getValueAsync(context)
        _selectedAppsFlow.value.clear()
        refreshNotifications()
    }

    private suspend fun loadSelectedApps(context: Context) {
        val apps = mutableListOf<DPackage>()
        filterData.value.apps.forEach { packageName ->
            try {
                val app = PackageHelper.getPackage(packageName)
                apps.add(app)
            } catch (e: Exception) {
                // App might be uninstalled, remove from list
                NotificationFilterPreference.toggleAppAsync(context, packageName)
            }
        }
        _selectedAppsFlow.value = apps.sortedBy { it.name }.toMutableStateList()
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