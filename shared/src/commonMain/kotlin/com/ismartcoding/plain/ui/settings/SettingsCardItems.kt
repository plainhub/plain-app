package com.ismartcoding.plain.ui.settings

import com.ismartcoding.plain.ui.components.PSwitch
import com.ismartcoding.plain.ui.media.extensions.collectAsStateValue
import androidx.compose.foundation.layout.padding
import com.ismartcoding.plain.ui.theme.PlainTheme
import com.ismartcoding.plain.preferences.*

import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.features.upgrade.LegacyDataUpgradeClient
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.ismartcoding.plain.ui.theme.DarkTheme
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.navigation.Routing
import androidx.navigation.NavHostController
import com.ismartcoding.plain.Prefs
import kotlinx.coroutines.launch
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.database_backup as ui_drawable_database_backup
import com.ismartcoding.plain.ui.resources.languages as ui_drawable_languages
import com.ismartcoding.plain.ui.resources.sun_moon as ui_drawable_sun_moon

@Composable
internal fun SettingsCardItems(navController: NavHostController) {
    val darkTheme = Prefs.darkTheme.collectAsStateValue()
    val scope = rememberCoroutineScope()

    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
        PListItem(
            modifier = Modifier.clickable {
                navController.navigate(Routing.DarkTheme)
            },
            icon = UiRes.drawable.ui_drawable_sun_moon,
            title = stringResource(Res.string.dark_theme),
            subtitle = DarkTheme.entries.find { it == darkTheme }?.getText() ?: "",
            separatedActions = true,
        ) {
            PSwitch(
                checked = DarkTheme.isDarkTheme(darkTheme),
                onCheckedChange = {
                    scope.launch {
                        Prefs.set(PrefKeys.DARK_THEME, (if (it) DarkTheme.ON else DarkTheme.OFF).name)
                    }
                },
            )
        }
    }
    VerticalSpace(dp = 16.dp)
    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
        PListItem(
            modifier = Modifier.clickable {
                navController.navigate(Routing.Language)
            },
            title = stringResource(Res.string.language),
            subtitle = Prefs.parseLocale(Prefs.locale.collectAsStateValue())?.getElegantDisplayName() ?: stringResource(Res.string.use_device_language),
            icon = UiRes.drawable.ui_drawable_languages,
            showMore = true,
        )
    }
    VerticalSpace(16.dp)
    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
        PListItem(
            modifier = Modifier.clickable {
                navController.navigate(Routing.BackupRestore)
            },
            title = stringResource(Res.string.backup_restore),
            subtitle = stringResource(Res.string.backup_desc),
            icon = UiRes.drawable.ui_drawable_database_backup,
            showMore = true,
        )
        if (LegacyDataUpgradeClient.supported) {
            PListItem(
                modifier = Modifier.clickable { navController.navigate(Routing.LegacyDataUpgrade()) },
                title = stringResource(Res.string.legacy_data_upgrade),
                showMore = true,
            )
        }
    }
}
