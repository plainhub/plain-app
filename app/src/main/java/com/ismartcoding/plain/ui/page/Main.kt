package com.ismartcoding.plain.ui.page

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ismartcoding.plain.data.enums.DarkTheme
import com.ismartcoding.plain.data.preference.LocalDarkTheme
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.models.SharedViewModel
import com.ismartcoding.plain.ui.page.scan.ScanHistoryPage
import com.ismartcoding.plain.ui.page.scan.ScanPage
import com.ismartcoding.plain.ui.page.settings.AboutPage
import com.ismartcoding.plain.ui.page.settings.BackupRestorePage
import com.ismartcoding.plain.ui.page.settings.ColorAndStylePage
import com.ismartcoding.plain.ui.page.settings.DarkThemePage
import com.ismartcoding.plain.ui.page.settings.LanguagePage
import com.ismartcoding.plain.ui.page.settings.LogsPage
import com.ismartcoding.plain.ui.page.settings.SettingsPage
import com.ismartcoding.plain.ui.page.tools.ExchangeRatePage
import com.ismartcoding.plain.ui.page.tools.SoundMeterPage
import com.ismartcoding.plain.ui.page.web.PasswordPage
import com.ismartcoding.plain.ui.page.web.SessionsPage
import com.ismartcoding.plain.ui.page.web.WebConsolePage
import com.ismartcoding.plain.ui.page.web.WebDevPage
import com.ismartcoding.plain.ui.theme.AppTheme
import com.ismartcoding.plain.ui.theme.backColor

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun Main(
    viewModel: MainViewModel,
) {
    val navController = rememberNavController()
    val useDarkTheme = DarkTheme.isDarkTheme(LocalDarkTheme.current)
    val sharedViewModel: SharedViewModel = viewModel()

    AppTheme(useDarkTheme = useDarkTheme) {
        val systemUiController = rememberSystemUiController()
        systemUiController.run {
            setStatusBarColor(Color.Transparent, !useDarkTheme)
            setSystemBarsColor(Color.Transparent, !useDarkTheme)
            setNavigationBarColor(MaterialTheme.colorScheme.backColor(), !useDarkTheme)
        }

        NavHost(
            modifier = Modifier.background(MaterialTheme.colorScheme.surface),
            navController = navController,
            startDestination = RouteName.HOME.name,
        ) {
            mapOf<RouteName, @Composable () -> Unit>(
                RouteName.HOME to { HomePage(navController, viewModel) },
                RouteName.SETTINGS to { SettingsPage(navController) },
                RouteName.COLOR_AND_STYLE to { ColorAndStylePage(navController) },
                RouteName.DARK_THEME to { DarkThemePage(navController) },
                RouteName.LANGUAGE to { LanguagePage(navController) },
                RouteName.BACKUP_RESTORE to { BackupRestorePage(navController) },
                RouteName.ABOUT to { AboutPage(navController) },
                RouteName.LOGS to { LogsPage(navController) },
                RouteName.WEB_CONSOLE to { WebConsolePage(navController, sharedViewModel) },
                RouteName.PASSWORD to { PasswordPage(navController) },
                RouteName.SESSIONS to { SessionsPage(navController) },
                RouteName.WEB_DEV to { WebDevPage(navController) },
                RouteName.EXCHANGE_RATE to { ExchangeRatePage(navController) },
                RouteName.SOUND_METER to { SoundMeterPage(navController) },
                RouteName.CHAT to { ChatPage(navController, sharedViewModel) },
                RouteName.CHAT_TEXT to { ChatTextPage(navController, sharedViewModel) },
                RouteName.TEXT to { TextPage(navController, sharedViewModel) },
                RouteName.SCAN to { ScanPage(navController) },
                RouteName.SCAN_HISTORY to { ScanHistoryPage(navController) },
            ).forEach { (routeName, content) ->
                composable(routeName.name) {
                    content()
                }
            }
        }
    }
}

