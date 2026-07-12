package com.ismartcoding.plain.ui.page.settings
import com.ismartcoding.plain.preferences.*

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.launch

@Composable
internal fun SettingsCardItems(navController: NavHostController) {
    val darkTheme = LocalDarkTheme.current
    val scope = rememberCoroutineScope()

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
                    DarkThemePreference.putAsync(if (it) DarkTheme.ON.value else DarkTheme.OFF.value)
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
