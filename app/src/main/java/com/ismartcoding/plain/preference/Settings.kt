package com.ismartcoding.plain.preference

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
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
    val newVersion: String,
    val skipVersion: String,
    val newVersionPublishDate: String,
    val newVersionLog: String,
    val newVersionSize: Long,
    val newVersionDownloadUrl: String,
)

val LocalThemeIndex = compositionLocalOf { ThemeIndexPreference.default }
val LocalCustomPrimaryColor = compositionLocalOf { CustomPrimaryColorPreference.default }
val LocalDarkTheme = compositionLocalOf { DarkThemePreference.default }
val LocalAmoledDarkTheme = compositionLocalOf { AmoledDarkThemePreference.default }
val LocalLocale = compositionLocalOf<Locale?> { null }
val LocalWeb = compositionLocalOf { WebPreference.default }
val LocalKeepScreenOn = compositionLocalOf { KeepScreenOnPreference.default }
val LocalSystemScreenTimeout = compositionLocalOf { SystemScreenTimeoutPreference.default }

// Version
val LocalNewVersion = compositionLocalOf { NewVersionPreference.default }
val LocalSkipVersion = compositionLocalOf { SkipVersionPreference.default }
val LocalNewVersionPublishDate = compositionLocalOf { NewVersionPublishDatePreference.default }
val LocalNewVersionLog = compositionLocalOf { NewVersionLogPreference.default }
val LocalNewVersionSize = compositionLocalOf { NewVersionSizePreference.default }
val LocalNewVersionDownloadUrl = compositionLocalOf { NewVersionDownloadUrlPreference.default }


@Composable
fun SettingsProvider(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val defaultSettings =
        Settings(
            themeIndex = ThemeIndexPreference.default,
            customPrimaryColor = CustomPrimaryColorPreference.default,
            darkTheme = DarkThemePreference.default,
            amoledDarkTheme = AmoledDarkThemePreference.default,
            locale = null,
            web = WebPreference.default,
            keepScreenOn = KeepScreenOnPreference.default,
            systemScreenTimeout = SystemScreenTimeoutPreference.default,
            newVersion = NewVersionPreference.default,
            skipVersion = SkipVersionPreference.default,
            newVersionPublishDate = NewVersionPublishDatePreference.default,
            newVersionLog = NewVersionLogPreference.default,
            newVersionSize = NewVersionSizePreference.default,
            newVersionDownloadUrl = NewVersionDownloadUrlPreference.default,
        )
    val settings =
        remember {
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
                      newVersion = NewVersionPreference.get(it),
                    skipVersion = SkipVersionPreference.get(it),
                    newVersionPublishDate = NewVersionPublishDatePreference.get(it),
                    newVersionLog = NewVersionLogPreference.get(it),
                    newVersionSize = NewVersionSizePreference.get(it),
                    newVersionDownloadUrl = NewVersionDownloadUrlPreference.get(it),
                )
            }
        }.collectAsStateValue(
            initial = defaultSettings,
        )

    CompositionLocalProvider(
        LocalThemeIndex provides settings.themeIndex,
        LocalCustomPrimaryColor provides settings.customPrimaryColor,
        LocalDarkTheme provides settings.darkTheme,
        LocalAmoledDarkTheme provides settings.amoledDarkTheme,
        LocalLocale provides settings.locale,
        LocalWeb provides settings.web,
        LocalKeepScreenOn provides settings.keepScreenOn,
        LocalSystemScreenTimeout provides settings.systemScreenTimeout,
        LocalNewVersion provides settings.newVersion,
        LocalSkipVersion provides settings.skipVersion,
        LocalNewVersionPublishDate provides settings.newVersionPublishDate,
        LocalNewVersionLog provides settings.newVersionLog,
        LocalNewVersionSize provides settings.newVersionSize,
        LocalNewVersionDownloadUrl provides settings.newVersionDownloadUrl,
    ) {
        content()
    }
}
