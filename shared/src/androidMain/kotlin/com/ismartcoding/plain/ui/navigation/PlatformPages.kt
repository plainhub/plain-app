package com.ismartcoding.plain.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.platform.system.Capability
import com.ismartcoding.plain.platform.system.getDeviceCapabilities
import com.ismartcoding.plain.platform.system.has
import com.ismartcoding.plain.ui.settings.WifiAwareDebugPage
import com.ismartcoding.plain.ui.tools.LauncherShortcutsPage
import com.ismartcoding.plain.ui.web.NotificationSettingsPage

internal fun NavGraphBuilder.addPlatformPages(navController: NavHostController) {
    if (AppFeatureType.NOTIFICATIONS.has()) {
        composable<Routing.NotificationSettings> { NotificationSettingsPage(navController) }
    }
    val capabilities = getDeviceCapabilities()
    if (Capability.WIFI_AWARE in capabilities) {
        composable<Routing.WifiAwareDebug> { WifiAwareDebugPage(navController) }
    }
    if (Capability.LAUNCHER_SHORTCUTS in capabilities) {
        composable<Routing.LauncherShortcuts> { LauncherShortcutsPage(navController) }
    }
}
