package com.ismartcoding.plain.ui.page.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.events.AppEvents
import com.ismartcoding.plain.extensions.getText
import com.ismartcoding.plain.preferences.DarkThemePreference
import com.ismartcoding.plain.preferences.LocalDarkTheme
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PSwitch
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.nav.Routing
import androidx.navigation.NavHostController
import com.ismartcoding.plain.TempData
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
            icon = R.drawable.sun_moon,
            title = stringResource(R.string.dark_theme),
            subtitle = DarkTheme.entries.find { it.value == darkTheme }?.getText(context) ?: "",
            separatedActions = true,
        ) {
            PSwitch(
                activated = DarkTheme.isDarkTheme(darkTheme),
            ) {
                scope.launch {
                    withIO {
                        DarkThemePreference.putAsync(context, if (it) DarkTheme.ON else DarkTheme.OFF)
                    }
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
            title = stringResource(R.string.language),
            subtitle = stringResource(R.string.language_desc),
            icon = R.drawable.languages,
            showMore = true,
        )
    }
    VerticalSpace(16.dp)
    PCard {
        PListItem(
            modifier = Modifier.clickable {
                navController.navigate(Routing.BackupRestore)
            },
            title = stringResource(R.string.backup_restore),
            subtitle = stringResource(R.string.backup_desc),
            icon = R.drawable.database_backup,
            showMore = true,
        )
    }
}

@Composable
internal fun DeveloperSettingsCard(navController: NavHostController) {
    PCard {
        PListItem(title = stringResource(R.string.client_id), value = TempData.clientId)
        PListItem(
            modifier = Modifier.clickable { navController.navigate(Routing.WebDev) },
            title = stringResource(R.string.developer_options),
            icon = R.drawable.code,
            showMore = true,
        )
        PListItem(
            modifier = Modifier.clickable { navController.navigate(Routing.ComponentShowcase) },
            title = stringResource(R.string.ui_components),
            icon = R.drawable.layout_grid,
            showMore = true,
        )
        PListItem(
            modifier = Modifier.clickable { throw RuntimeException("Test crash triggered from Developer Options") },
            title = stringResource(R.string.simulate_crash),
            subtitle = stringResource(R.string.simulate_crash_desc),
            icon = R.drawable.circle_alert,
        )
    }
}
