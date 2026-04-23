package com.ismartcoding.plain.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.ismartcoding.plain.data.DUpdateInfo
import com.ismartcoding.plain.ui.extensions.collectAsStateValue
import kotlinx.coroutines.flow.map
import java.util.Locale

data class Settings(
    val themeIndex: Int,
    val customPrimaryColor: String,
    val darkTheme: Int,
    val amoledDarkTheme: Boolean,
    val locale: Locale?,
    val web: Boolean,
    val keepScreenOn: Boolean,
    val systemScreenTimeout: Int,
    val updateInfo: DUpdateInfo,
)

val LocalThemeIndex = compositionLocalOf { ThemeIndexPreference.default }
val LocalCustomPrimaryColor = compositionLocalOf { CustomPrimaryColorPreference.default }
val LocalDarkTheme = compositionLocalOf { DarkThemePreference.default }
val LocalAmoledDarkTheme = compositionLocalOf { AmoledDarkThemePreference.default }
val LocalLocale = compositionLocalOf<Locale?> { null }
val LocalWeb = compositionLocalOf { WebPreference.default }
val LocalKeepScreenOn = compositionLocalOf { KeepScreenOnPreference.default }
val LocalSystemScreenTimeout = compositionLocalOf { SystemScreenTimeoutPreference.default }
val LocalUpdateInfo = compositionLocalOf { DUpdateInfo() }

// Convenience accessors for individual update fields
val LocalNewVersion = compositionLocalOf { "" }
val LocalSkipVersion = compositionLocalOf { "" }
val LocalNewVersionPublishDate = compositionLocalOf { "" }
val LocalNewVersionLog = compositionLocalOf { "" }
val LocalNewVersionSize = compositionLocalOf { 0L }
val LocalAutoCheckUpdate = compositionLocalOf { true }

@Composable
fun SettingsProvider(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val defaultSettings = Settings(
        themeIndex = ThemeIndexPreference.default,
        customPrimaryColor = CustomPrimaryColorPreference.default,
        darkTheme = DarkThemePreference.default,
        amoledDarkTheme = AmoledDarkThemePreference.default,
        locale = null,
        web = WebPreference.default,
        keepScreenOn = KeepScreenOnPreference.default,
        systemScreenTimeout = SystemScreenTimeoutPreference.default,
        updateInfo = DUpdateInfo(),
    )
    val settings = remember {
        context.dataStore.dataFlow.map {
            Settings(
                themeIndex = ThemeIndexPreference.get(it),
                customPrimaryColor = CustomPrimaryColorPreference.get(it),
                darkTheme = DarkThemePreference.get(it),
                amoledDarkTheme = AmoledDarkThemePreference.get(it),
                locale = LanguagePreference.getLocale(it),
                web = WebPreference.get(it),
                keepScreenOn = KeepScreenOnPreference.get(it),
                systemScreenTimeout = SystemScreenTimeoutPreference.get(it),
                updateInfo = UpdateInfoPreference.getValue(it),
            )
        }
    }.collectAsStateValue(initial = defaultSettings)

    CompositionLocalProvider(
        LocalThemeIndex provides settings.themeIndex,
        LocalCustomPrimaryColor provides settings.customPrimaryColor,
        LocalDarkTheme provides settings.darkTheme,
        LocalAmoledDarkTheme provides settings.amoledDarkTheme,
        LocalLocale provides settings.locale,
        LocalWeb provides settings.web,
        LocalKeepScreenOn provides settings.keepScreenOn,
        LocalSystemScreenTimeout provides settings.systemScreenTimeout,
        LocalUpdateInfo provides settings.updateInfo,
        LocalNewVersion provides settings.updateInfo.newVersion,
        LocalSkipVersion provides settings.updateInfo.skipVersion,
        LocalNewVersionPublishDate provides settings.updateInfo.publishDate,
        LocalNewVersionLog provides settings.updateInfo.log,
        LocalNewVersionSize provides settings.updateInfo.size,
        LocalAutoCheckUpdate provides settings.updateInfo.autoCheckUpdate,
    ) {
        content()
    }
}
