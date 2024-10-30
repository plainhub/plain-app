package com.ismartcoding.plain.ui.page.feeds

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.features.feed.FeedAutoRefreshInterval
import com.ismartcoding.plain.helpers.FormatHelper
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PBlockButton
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PDialogRadioRow
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PSwitch
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.RadioDialog
import com.ismartcoding.plain.ui.base.RadioDialogOption
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.FeedSettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedSettingsPage(
    navController: NavHostController,
    feedSettingsVM: FeedSettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        feedSettingsVM.loadSettings(context)
    }

    if (feedSettingsVM.showIntervalDialog.value) {
        val options = remember {
            setOf(900, 1800, 3600, 7200, 21600, 43200, 86400).map { FeedAutoRefreshInterval(it) }
        }

        RadioDialog(
            title = stringResource(R.string.auto_refresh_interval),
            options = options.map {
                RadioDialogOption(
                    text = it.getText(),
                    selected = it.value == feedSettingsVM.autoRefreshInterval.intValue,
                ) {
                    feedSettingsVM.setAutoRefreshInterval(context, it.value)
                }
            },
        ) {
            feedSettingsVM.showIntervalDialog.value = false
        }
    }

    if (feedSettingsVM.showClearFeedsDialog.value) {
        ClearFeedsDialog(feedSettingsVM)
    }

    PScaffold(
        topBar = {
            PTopAppBar(navController = navController, title = stringResource(id = R.string.settings))
        },
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
            item {
                TopSpace()
            }
            item {
                PCard {
                    PListItem(
                        modifier = Modifier.clickable {
                            feedSettingsVM.setAutoRefresh(context, !feedSettingsVM.autoRefresh.value)
                        },
                        title = stringResource(id = R.string.auto_refresh_feeds),
                    ) {
                        PSwitch(
                            activated = feedSettingsVM.autoRefresh.value,
                        ) {
                            feedSettingsVM.setAutoRefresh(context, it)
                        }
                    }

                    if (feedSettingsVM.autoRefresh.value) {
                        PListItem(
                            modifier = Modifier.clickable {
                                feedSettingsVM.showIntervalDialog.value = true
                            },
                            title = stringResource(id = R.string.auto_refresh_interval),
                            value = FormatHelper.formatSeconds(feedSettingsVM.autoRefreshInterval.intValue),
                            showMore = true,
                        )
                        PListItem(
                            modifier = Modifier.clickable {
                                feedSettingsVM.setAutoRefreshOnlyWifi(context, !feedSettingsVM.autoRefreshOnlyWifi.value)
                            },
                            title = stringResource(id = R.string.auto_refresh_only_over_wifi),
                        ) {
                            PSwitch(
                                activated = feedSettingsVM.autoRefreshOnlyWifi.value,
                            ) {
                                feedSettingsVM.setAutoRefreshOnlyWifi(context, it)
                            }
                        }
                    }
                }
            }
            item {
                VerticalSpace(dp = 48.dp)
                PBlockButton(text = stringResource(id = R.string.clear_feed_items), type = ButtonType.DANGER, onClick = {
                    feedSettingsVM.showClearFeedsDialog.value = true
                })
            }
            item {
                BottomSpace()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ClearFeedsDialog(
    feedSettingsVM: FeedSettingsViewModel,
) {
    val scope = rememberCoroutineScope()
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = {
            feedSettingsVM.showClearFeedsDialog.value = false
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        DialogHelper.showLoading()
                        withIO {
                            if (feedSettingsVM.clearFeedItemsTs.longValue == 0L) {
                                feedSettingsVM.clearAllAsync()
                            } else {
                                feedSettingsVM.clearByTimeAsync(feedSettingsVM.clearFeedItemsTs.longValue)
                            }
                        }
                        DialogHelper.hideLoading()
                        feedSettingsVM.showClearFeedsDialog.value = false
                        DialogHelper.showMessage(R.string.feed_items_cleared)
                    }
                }
            ) {
                Text(stringResource(id = R.string.clear))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                feedSettingsVM.showClearFeedsDialog.value = false
            }) {
                Text(stringResource(id = R.string.cancel))
            }
        },
        title = {
            Text(
                text = stringResource(id = R.string.clear_feed_items),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                PDialogRadioRow(selected = feedSettingsVM.clearFeedItemsTs.longValue == 0L, onClick = {
                    feedSettingsVM.clearFeedItemsTs.longValue = 0
                }, text = stringResource(id = R.string.all))
                PDialogRadioRow(selected = feedSettingsVM.clearFeedItemsTs.longValue == Constants.ONE_DAY * 7, onClick = {
                    feedSettingsVM.clearFeedItemsTs.longValue = Constants.ONE_DAY * 7
                }, text = stringResource(id = R.string.older_than_7days_feed_items))
                PDialogRadioRow(selected = feedSettingsVM.clearFeedItemsTs.longValue == Constants.ONE_DAY * 30, onClick = {
                    feedSettingsVM.clearFeedItemsTs.value = Constants.ONE_DAY * 30
                }, text = stringResource(id = R.string.older_than_30days_feed_items))
            }
        })
}