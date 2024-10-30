package com.ismartcoding.plain.ui.page.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.Version
import com.ismartcoding.plain.data.toVersion
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.extensions.getText
import com.ismartcoding.plain.features.AppEvents
import com.ismartcoding.plain.preference.DarkThemePreference
import com.ismartcoding.plain.preference.LocalDarkTheme
import com.ismartcoding.plain.preference.LocalNewVersion
import com.ismartcoding.plain.preference.LocalSkipVersion
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PBanner
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PSwitch
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.models.UpdateViewModel
import com.ismartcoding.plain.ui.nav.Routing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(navController: NavHostController, updateViewModel: UpdateViewModel = viewModel()) {
    val currentVersion = Version(BuildConfig.VERSION_NAME)
    val newVersion = LocalNewVersion.current.toVersion()
    val skipVersion = LocalSkipVersion.current.toVersion()
    val darkTheme = LocalDarkTheme.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    UpdateDialog(updateViewModel)

    PScaffold(
        topBar = {
            PTopAppBar(navController = navController, title = stringResource(R.string.settings))
        },
        content = { paddingValues ->
            LazyColumn(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
                item {
                    TopSpace()
                }
                item {
                    if (AppFeatureType.CHECK_UPDATES.has() && newVersion.whetherNeedUpdate(currentVersion, skipVersion)) {
                        PBanner(
                            title = stringResource(R.string.get_new_updates, newVersion.toString()),
                            desc = stringResource(
                                R.string.get_new_updates_desc
                            ),
                            icon = R.drawable.lightbulb,
                        ) {
                            updateViewModel.showDialog()
                        }
                        VerticalSpace(dp = 16.dp)
                    }
                }
                item {
                    PCard {
                        PListItem(
                            modifier = Modifier.clickable {
                                navController.navigate(Routing.DarkTheme)
                            },
                            icon = R.drawable.sun_moon,
                            title = stringResource(R.string.dark_theme),
                            desc = DarkTheme.entries.find { it.value == darkTheme }?.getText(context) ?: "",
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
                            desc = stringResource(R.string.language_desc),
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
                            desc = stringResource(R.string.backup_desc),
                            icon = R.drawable.database_backup,
                            showMore = true,
                        )
                        PListItem(
                            modifier = Modifier.clickable {
                                navController.navigate(Routing.About)
                            },
                            title = stringResource(R.string.about),
                            desc = stringResource(R.string.about_desc),
                            icon = R.drawable.lightbulb,
                            showMore = true,
                        )
                    }
                }
                if (BuildConfig.DEBUG) {
                    item {
                        VerticalSpace(16.dp)
                        PCard {
                            PListItem(
                                title = "WAKE LOCK",
                                value = AppEvents.wakeLock.isHeld.getText(),
                            )
                        }
                    }
                }
                item {
                    BottomSpace()
                }
            }
        },
    )
}
