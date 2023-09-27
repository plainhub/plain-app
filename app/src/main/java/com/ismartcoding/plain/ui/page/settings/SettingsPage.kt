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
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.SelectableGroupItem
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.extensions.navigate
import com.ismartcoding.plain.ui.page.RouteName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(navController: NavHostController) {
    PScaffold(
        navController,
        topBarTitle = stringResource(R.string.settings),
        content = {
            LazyColumn {
                item {
                    VerticalSpace(dp = 16.dp)
                }
                item {
                    SelectableGroupItem(
                        title = stringResource(R.string.color_and_style),
                        desc = stringResource(R.string.color_and_style_desc),
                        icon = Icons.Outlined.Palette,
                        showMore = true,
                        onClick = {
                            navController.navigate(RouteName.COLOR_AND_STYLE)
                        },
                    )
                    SelectableGroupItem(
                        title = stringResource(R.string.language),
                        desc = stringResource(R.string.language_desc),
                        icon = Icons.Outlined.Language,
                        showMore = true,
                        onClick = {
                            navController.navigate(RouteName.LANGUAGE)
                        },
                    )
                    SelectableGroupItem(
                        title = stringResource(R.string.backup_restore),
                        desc = stringResource(R.string.backup_desc),
                        icon = Icons.Outlined.Backup,
                        showMore = true,
                        onClick = {
                            navController.navigate(RouteName.BACKUP_RESTORE)
                        },
                    )
                    SelectableGroupItem(
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
        },
    )
}
