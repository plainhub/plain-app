package com.ismartcoding.plain.ui.page.settings

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.TipsAndUpdates
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.SelectableGroupItem
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.extensions.navigate
import com.ismartcoding.plain.ui.page.RouteName
import com.ismartcoding.plain.ui.theme.PlainTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(navController: NavHostController) {
    PScaffold(
        navController,
        topBarTitle = stringResource(R.string.settings),
        content = {
            LazyColumn {
                item {
                    TopSpace()
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
            }
        },
    )
}
