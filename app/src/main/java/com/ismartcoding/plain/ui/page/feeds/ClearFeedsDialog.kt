package com.ismartcoding.plain.ui.page.feeds

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.ui.base.PDialogRadioRow
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.FeedSettingsViewModel
import kotlinx.coroutines.launch

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
                        DialogHelper.showMessage(Res.string.feed_items_cleared)
                    }
                }
            ) {
                Text(stringResource(Res.string.clear))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                feedSettingsVM.showClearFeedsDialog.value = false
            }) {
                Text(stringResource(Res.string.cancel))
            }
        },
        title = {
            Text(
                text = stringResource(Res.string.clear_feed_items),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                PDialogRadioRow(selected = feedSettingsVM.clearFeedItemsTs.longValue == 0L, onClick = {
                    feedSettingsVM.clearFeedItemsTs.longValue = 0
                }, text = stringResource(Res.string.all))
                PDialogRadioRow(selected = feedSettingsVM.clearFeedItemsTs.longValue == Constants.ONE_DAY * 7, onClick = {
                    feedSettingsVM.clearFeedItemsTs.longValue = Constants.ONE_DAY * 7
                }, text = stringResource(Res.string.older_than_7days_feed_items))
                PDialogRadioRow(selected = feedSettingsVM.clearFeedItemsTs.longValue == Constants.ONE_DAY * 30, onClick = {
                    feedSettingsVM.clearFeedItemsTs.value = Constants.ONE_DAY * 30
                }, text = stringResource(Res.string.older_than_30days_feed_items))
            }
        })
}
