package com.ismartcoding.plain.ui.page.feeds

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.feed.FeedAutoRefreshInterval
import com.ismartcoding.plain.helpers.FormatHelper
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PSwitch
import com.ismartcoding.plain.ui.base.RadioDialog
import com.ismartcoding.plain.ui.base.RadioDialogOption
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.models.FeedSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedSettingsPage(
    navController: NavHostController,
    viewModel: FeedSettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.loadSettings(context)
    }

    if (viewModel.showIntervalDialog.value) {
        val options = remember {
            setOf(900, 1800, 3600, 7200, 21600, 43200, 86400).map { FeedAutoRefreshInterval(it) }
        }

        RadioDialog(
            title = stringResource(R.string.auto_refresh_interval),
            options = options.map {
                RadioDialogOption(
                    text = it.getText(),
                    selected = it.value == viewModel.autoRefreshInterval.value,
                ) {
                    viewModel.setAutoRefreshInterval(context, it.value)
                }
            },
        ) {
            viewModel.showIntervalDialog.value = false
        }
    }

    PScaffold(
        navController,
        topBarTitle = stringResource(id = R.string.settings),
    ) {
        LazyColumn {
            item {
                TopSpace()
            }
            item {
                PCard {

                    PListItem(
                        title = stringResource(id = R.string.auto_refresh_feeds),
                        onClick = {
                            viewModel.setAutoRefresh(context, !viewModel.autoRefresh.value)
                        }
                    ) {
                        PSwitch(
                            activated = viewModel.autoRefresh.value,
                        ) {
                            viewModel.setAutoRefresh(context, it)
                        }
                    }

                    if (viewModel.autoRefresh.value) {
                        PListItem(
                            title = stringResource(id = R.string.auto_refresh_interval),
                            value = FormatHelper.formatSeconds(viewModel.autoRefreshInterval.value),
                            showMore = true,
                            onClick = {
                                viewModel.showIntervalDialog.value = true
                            }
                        )
                        PListItem(
                            title = stringResource(id = R.string.auto_refresh_only_over_wifi),
                            onClick = {
                                viewModel.setAutoRefreshOnlyWifi(context, !viewModel.autoRefreshOnlyWifi.value)
                            }
                        ) {
                            PSwitch(
                                activated = viewModel.autoRefreshOnlyWifi.value,
                            ) {
                                viewModel.setAutoRefreshOnlyWifi(context, it)
                            }
                        }
                    }
                }
            }
            item {
                BottomSpace()
            }
        }
    }
}