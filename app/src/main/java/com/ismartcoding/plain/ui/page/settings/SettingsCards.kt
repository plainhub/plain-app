package com.ismartcoding.plain.ui.page.settings
import com.ismartcoding.plain.preferences.*

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.preferences.DarkThemePreference
import com.ismartcoding.plain.preferences.LocalDarkTheme
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PSwitch
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.nav.Routing
import androidx.navigation.NavHostController
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.peer.transport.WifiAwareTransport
import com.ismartcoding.plain.lib.isTPlus
import com.ismartcoding.plain.wifiAwareManager
import android.net.wifi.aware.Characteristics
import androidx.compose.runtime.remember
import kotlinx.coroutines.launch

@Composable
internal fun SettingsCardItems(navController: NavHostController) {
    val darkTheme = LocalDarkTheme.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    PCard {
        PListItem(
            modifier = Modifier.clickable {
                navController.navigate(Routing.DarkTheme)
            },
            icon = Res.drawable.sun_moon,
            title = stringResource(Res.string.dark_theme),
            subtitle = DarkTheme.entries.find { it.value == darkTheme }?.getText() ?: "",
            separatedActions = true,
        ) {
            PSwitch(
                activated = DarkTheme.isDarkTheme(darkTheme),
            ) {
                scope.launch {
                    DarkThemePreference.putAsync(if (it) DarkTheme.ON else DarkTheme.OFF)
                }
            }
        }
    }
    VerticalSpace(dp = 16.dp)
    PCard {
        PListItem(
            modifier = Modifier.clickable {
                navController.navigate(Routing.Language)
            },
            title = stringResource(Res.string.language),
            subtitle = stringResource(Res.string.language_desc),
            icon = Res.drawable.languages,
            showMore = true,
        )
    }
    VerticalSpace(16.dp)
    PCard {
        PListItem(
            modifier = Modifier.clickable {
                navController.navigate(Routing.BackupRestore)
            },
            title = stringResource(Res.string.backup_restore),
            subtitle = stringResource(Res.string.backup_desc),
            icon = Res.drawable.database_backup,
            showMore = true,
        )
    }
}

@Composable
internal fun DeveloperSettingsCard(navController: NavHostController) {
    PCard {
        PListItem(title = stringResource(Res.string.client_id), value = TempData.clientId)
        if (WifiAwareTransport.isSupported()) {
            val chars = rememberAwareCharacteristics()
            PListItem(
                title = stringResource(Res.string.aware_data_interfaces),
                value = chars?.getNumberOfSupportedDataInterfaces()?.toString()
                    ?: stringResource(Res.string.not_available),
            )
            PListItem(
                title = stringResource(Res.string.aware_data_paths),
                value = chars?.getNumberOfSupportedDataPaths()?.toString()
                    ?: stringResource(Res.string.not_available),
            )
        }
        PListItem(
            modifier = Modifier.clickable { navController.navigate(Routing.WebDev) },
            title = stringResource(Res.string.developer_options),
            icon = Res.drawable.code,
            showMore = true,
        )
        PListItem(
            modifier = Modifier.clickable { navController.navigate(Routing.ComponentShowcase) },
            title = stringResource(Res.string.ui_components),
            icon = Res.drawable.layout_grid,
            showMore = true,
        )
        PListItem(
            modifier = Modifier.clickable { throw RuntimeException("Test crash triggered from Developer Options") },
            title = stringResource(Res.string.simulate_crash),
            subtitle = stringResource(Res.string.simulate_crash_desc),
            icon = Res.drawable.circle_alert,
        )
    }
}

@Composable
private fun rememberAwareCharacteristics(): Characteristics? {
    if (!isTPlus()) return null
    return remember {
        runCatching { wifiAwareManager.getCharacteristics() }.getOrNull()
    }
}
