package com.ismartcoding.plain.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.platform.system.Capability
import com.ismartcoding.plain.platform.system.getDeviceCapabilities
import com.ismartcoding.plain.platform.system.has
import com.ismartcoding.plain.ui.settings.WifiAwareDebugPage
import com.ismartcoding.plain.ui.web.NotificationSettingsPage
import com.ismartcoding.plain.ui.web.ShizukuSetupPage

internal fun NavGraphBuilder.addPlatformPages(navController: NavHostController) {
    if (AppFeatureType.NOTIFICATIONS.has()) {
        composable<Routing.NotificationSettings> { NotificationSettingsPage(navController) }
    }
    val capabilities = getDeviceCapabilities()
    if (Capability.ADB in capabilities) {
        composable<Routing.ShizukuSetup> { ShizukuSetupPage(navController) }
    }
    if (Capability.WIFI_AWARE in capabilities) {
        composable<Routing.WifiAwareDebug> { WifiAwareDebugPage(navController) }
    }
}
