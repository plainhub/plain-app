package com.ismartcoding.plain.ui.page.settings

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.TipsAndUpdates
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.data.Version
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.preference.LocalNewVersion
import com.ismartcoding.plain.preference.LocalSkipVersion
import com.ismartcoding.plain.data.toVersion
import com.ismartcoding.plain.extensions.getText
import com.ismartcoding.plain.features.AppEvents
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PBanner
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PSwitch
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.extensions.navigate
import com.ismartcoding.plain.ui.models.UpdateViewModel
import com.ismartcoding.plain.ui.page.RouteName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(navController: NavHostController, updateViewModel: UpdateViewModel = viewModel()) {
    val currentVersion = Version(BuildConfig.VERSION_NAME)
    val newVersion = LocalNewVersion.current.toVersion()
    val skipVersion = LocalSkipVersion.current.toVersion()
    var demoMode by remember { mutableStateOf(TempData.demoMode) }

    UpdateDialog(updateViewModel)

    PScaffold(
        navController,
        topBarTitle = stringResource(R.string.settings),
        content = {
            LazyColumn {
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
                            icon = Icons.Outlined.Lightbulb,
                        ) {
                            updateViewModel.showDialog()
                        }
                        VerticalSpace(dp = 16.dp)
                    }
                }
                item {
                    PCard {
                        PListItem(
                            title = stringResource(R.string.color_and_style),
                            desc = stringResource(R.string.color_and_style_desc),
                            icon = Icons.Outlined.Palette,
                            showMore = true,
                            onClick = {
                                navController.navigate(RouteName.COLOR_AND_STYLE)
                            },
                        )
                        PListItem(
                            title = stringResource(R.string.language),
                            desc = stringResource(R.string.language_desc),
                            icon = Icons.Outlined.Language,
                            showMore = true,
                            onClick = {
                                navController.navigate(RouteName.LANGUAGE)
                            },
                        )
                    }
                    VerticalSpace(16.dp)
                    PCard {
                        PListItem(
                            title = stringResource(R.string.backup_restore),
                            desc = stringResource(R.string.backup_desc),
                            icon = Icons.Outlined.Backup,
                            showMore = true,
                            onClick = {
                                navController.navigate(RouteName.BACKUP_RESTORE)
                            },
                        )
                        PListItem(
                            title = stringResource(R.string.about),
                            desc = stringResource(R.string.about_desc),
                            icon = Icons.Outlined.TipsAndUpdates,
                            showMore = true,
                            onClick = {
                                navController.navigate(RouteName.ABOUT)
                            },
                        )
                    }
                }
                if (BuildConfig.DEBUG) {
                    item {
                        VerticalSpace(16.dp)
                        PCard {
                            PListItem(
                                title = stringResource(R.string.demo_mode),
                            ) {
                                PSwitch(
                                    activated = demoMode,
                                ) {
                                    demoMode = it
                                    TempData.demoMode = it
                                }
                            }
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
