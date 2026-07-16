package com.ismartcoding.plain.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import com.ismartcoding.plain.data.DUpdateInfo
import com.ismartcoding.plain.platform.Locale
import com.ismartcoding.plain.ui.extensions.collectAsStateValue
import kotlinx.coroutines.flow.map

data class Settings(
    val darkTheme: Int,
    val amoledDarkTheme: Boolean,
    val locale: Locale?,
    val web: Boolean,
    val updateInfo: DUpdateInfo,
)

val LocalLocale = compositionLocalOf<Locale?> { null }

@Composable
fun SettingsProvider(content: @Composable () -> Unit) {
    val defaultSettings = Settings(
        darkTheme = DarkThemePreference.default,
        amoledDarkTheme = AmoledDarkThemePreference.default,
        locale = null,
        web = WebPreference.default,
        updateInfo = DUpdateInfo(),
    )
    val settings = remember {
        appDataStore.dataFlow.map {
            Settings(
                darkTheme = DarkThemePreference.get(it),
                amoledDarkTheme = AmoledDarkThemePreference.get(it),
                locale = LanguagePreference.getLocale(it),
                web = WebPreference.get(it),
                updateInfo = UpdateInfoPreference.getValue(it),
            )
        }
    }.collectAsStateValue(initial = defaultSettings)

    CompositionLocalProvider(
        LocalDarkTheme provides settings.darkTheme,
        LocalAmoledDarkTheme provides settings.amoledDarkTheme,
        LocalLocale provides settings.locale,
        LocalWeb provides settings.web,
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
