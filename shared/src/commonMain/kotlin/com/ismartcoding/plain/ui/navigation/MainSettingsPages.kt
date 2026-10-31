package com.ismartcoding.plain.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.platform.system.getOwnPackageName
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.ui.chat.PeerViewModel
import com.ismartcoding.plain.ui.connections.ApiTokenTipsPage
import com.ismartcoding.plain.ui.connections.ConnectionsPage
import com.ismartcoding.plain.ui.onboarding.OnboardingPage
import com.ismartcoding.plain.ui.settings.AutoCheckUpdatePage
import com.ismartcoding.plain.ui.settings.BackupRestorePage
import com.ismartcoding.plain.ui.settings.BleDebugPage
import com.ismartcoding.plain.ui.settings.ComponentShowcasePage
import com.ismartcoding.plain.ui.settings.DarkThemePage
import com.ismartcoding.plain.ui.settings.DesktopAccessSettingsViewModel
import com.ismartcoding.plain.ui.settings.LanguagePage
import com.ismartcoding.plain.ui.settings.MarkdownThemePreviewPage
import com.ismartcoding.plain.ui.settings.MdnsDebugPage
import com.ismartcoding.plain.ui.settings.ServiceDebugPage
import com.ismartcoding.plain.ui.settings.SettingsPage
import com.ismartcoding.plain.ui.settings.UpdateViewModel
import com.ismartcoding.plain.ui.settings.developer.WebDevPage
import com.ismartcoding.plain.ui.tools.HomeFeaturesSelectionPage
import com.ismartcoding.plain.ui.web.ClipboardHistoryPage
import com.ismartcoding.plain.ui.web.DesktopAccessSettingsPage
import com.ismartcoding.plain.ui.web.HowToUsePage
import com.ismartcoding.plain.ui.web.ReplaceSslCertificatePage
import com.ismartcoding.plain.ui.web.WebSecurityPage

internal fun NavGraphBuilder.addMainSettingsPages(
    navController: NavHostController,
    updateVM: UpdateViewModel,
    peerVM: PeerViewModel,
    batterySettings: (@Composable () -> Unit)?,
) {
    composable<Routing.Settings> { SettingsPage(navController, updateVM, peerVM) }
    composable<Routing.DarkTheme> { DarkThemePage(navController) }
    composable<Routing.AutoCheckUpdate> { AutoCheckUpdatePage(navController, updateVM) }
    composable<Routing.MarkdownThemePreview> {
        MarkdownThemePreviewPage(navController)
    }
    composable<Routing.Language> { LanguagePage(navController) }
    composable<Routing.BackupRestore> { BackupRestorePage(navController) }
    composable<Routing.DesktopAccessSettings> { DesktopAccessSettingsPage(navController, batterySettings = batterySettings) }
    composable<Routing.ClipboardHistory> { ClipboardHistoryPage(navController) }
    composable<Routing.Connections> { ConnectionsPage(navController) }
    composable<Routing.ApiTokenTips> { backStackEntry ->
        val r = backStackEntry.toRoute<Routing.ApiTokenTips>()
        ApiTokenTipsPage(navController, r.clientId, r.token)
    }
    composable<Routing.WebDev> {
        WebDevPage(navController, packageId = getOwnPackageName(), onResetToken = {
            coIO { Prefs.resetAdbToken() }
        })
    }
    composable<Routing.WebSecurity> { WebSecurityPage(navController) }
    composable<Routing.ReplaceSslCertificate> { ReplaceSslCertificatePage(navController) }
    composable<Routing.HowToUse> {
        val webVM: DesktopAccessSettingsViewModel = viewModel { DesktopAccessSettingsViewModel() }
        HowToUsePage(navController, webVM)
    }
    composable<Routing.Onboarding> { OnboardingPage(navController) }
    composable<Routing.BleDebug> {
        BleDebugPage(navController)
    }
    composable<Routing.ServiceDebug> {
        ServiceDebugPage(navController)
    }
    composable<Routing.MdnsDebug> {
        MdnsDebugPage(navController)
    }
    composable<Routing.ComponentShowcase> { ComponentShowcasePage(navController) }
}
