package com.ismartcoding.plain.platform.system

import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.lib.coMain
import com.ismartcoding.plain.ui.app.MainViewModel
import com.ismartcoding.plain.ui.navigation.LauncherShortcutTools
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine

object LauncherShortcuts {
    internal fun observeSelection() = coMain {
        combine(Prefs.launcherShortcuts, Prefs.locale) { shortcuts, _ -> shortcuts }
            .collectLatest { publishLauncherShortcuts(Prefs.selectedLauncherShortcuts(it)) }
    }

    fun open(type: String): Boolean {
        val feature = LauncherShortcutTools.ALL.firstOrNull { it.name == type } ?: return false
        MainViewModel.openLauncherShortcut(feature)
        return true
    }
}

internal expect suspend fun publishLauncherShortcuts(tools: List<AppFeatureType>)
