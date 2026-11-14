package com.ismartcoding.plain.ui.settings

import com.ismartcoding.plain.features.notifications.NotificationFilterMode
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.features.files.FileSortBy
import com.ismartcoding.plain.features.packages.PackagesClient
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.models.Package as AppPackage
import com.ismartcoding.plain.platform.system.getOwnPackageName
import com.ismartcoding.plain.platform.system.getPackageInfoMap
import kotlinx.coroutines.flow.MutableStateFlow

class NotificationSettingsViewModel : ViewModel() {
    val selectedAppsFlow = MutableStateFlow<List<AppPackage>>(emptyList())
    val allAppsFlow = MutableStateFlow<List<AppPackage>>(emptyList())

    var filterData = mutableStateOf(Prefs.notificationFilter.value)
    var isLoading = mutableStateOf(true)
    var showAppSelector = mutableStateOf(false)
    var appsLoaded = mutableStateOf(false)
    var searchQuery = mutableStateOf("")

    val selectedAppIds = mutableStateListOf<String>()

    init {
        viewModelScope.launch {
            Prefs.notificationFilter.collect { filter ->
                filterData.value = filter
                try {
                    val apps = if (filter.apps.isEmpty()) emptyList()
                    else getPackageInfoMap(filter.apps.toList()).values.filterNotNull()
                    selectedAppsFlow.value = apps.sortedBy { it.name }
                    appsLoaded.value = false
                } catch (canceled: CancellationException) {
                    throw canceled
                } catch (error: Exception) {
                    LogCat.e("Notification filter apps", error)
                } finally {
                    isLoading.value = false
                }
            }
        }
    }

    suspend fun loadAllAppsAsync() {
        if (appsLoaded.value) return

        try {
            val ownPackage = getOwnPackageName()
            val apps = PackagesClient.search("", Int.MAX_VALUE, 0, FileSortBy.NAME_ASC)
                .filter { it.id != ownPackage && it.id !in filterData.value.apps }
            allAppsFlow.value = apps
            appsLoaded.value = true
        } catch (canceled: CancellationException) {
            throw canceled
        } catch (error: Exception) {
            appsLoaded.value = false
            LogCat.e("Notification app selection", error)
        }
    }

    suspend fun toggleModeAsync() {
        val newMode = if (filterData.value.mode == NotificationFilterMode.ALLOWLIST) {
            NotificationFilterMode.BLACKLIST
        } else {
            NotificationFilterMode.ALLOWLIST
        }
        Prefs.setNotificationMode(newMode)
    }

    suspend fun removeAppAsync(packageName: String) {
        Prefs.toggleNotificationApp(packageName)
    }

    suspend fun addAppsAsync(packageNames: List<String>) {
        packageNames.forEach { packageName ->
            Prefs.toggleNotificationApp(packageName)
        }
    }

    suspend fun clearAllAsync() {
        Prefs.clearNotificationApps()
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
