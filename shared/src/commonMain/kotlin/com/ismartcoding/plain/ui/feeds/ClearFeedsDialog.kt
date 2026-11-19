package com.ismartcoding.plain.ui.feeds

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.ui.components.RadioDialog
import com.ismartcoding.plain.ui.components.RadioDialogOption
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun ClearFeedsDialog(feedSettingsVM: FeedSettingsViewModel) {
    val scope = rememberCoroutineScope()
    RadioDialog(
        title = stringResource(Res.string.clear_feed_items),
        options = listOf(
            RadioDialogOption(
                text = stringResource(Res.string.all),
                selected = feedSettingsVM.clearFeedItemsTs.longValue == 0L,
                onClick = { feedSettingsVM.clearFeedItemsTs.longValue = 0L },
            ),
            RadioDialogOption(
                text = stringResource(Res.string.older_than_7days_feed_items),
                selected = feedSettingsVM.clearFeedItemsTs.longValue == Constants.ONE_DAY * 7,
                onClick = { feedSettingsVM.clearFeedItemsTs.longValue = Constants.ONE_DAY * 7 },
            ),
            RadioDialogOption(
                text = stringResource(Res.string.older_than_30days_feed_items),
                selected = feedSettingsVM.clearFeedItemsTs.longValue == Constants.ONE_DAY * 30,
                onClick = { feedSettingsVM.clearFeedItemsTs.longValue = Constants.ONE_DAY * 30 },
            ),
        ),
        confirmText = stringResource(Res.string.clear),
        onConfirm = {
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
        },
        onDismissRequest = { feedSettingsVM.showClearFeedsDialog.value = false },
    )
}
