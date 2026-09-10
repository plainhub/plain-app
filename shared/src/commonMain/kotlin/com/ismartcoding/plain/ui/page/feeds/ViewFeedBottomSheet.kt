package com.ismartcoding.plain.ui.page.feeds
import androidx.compose.foundation.layout.padding
import com.ismartcoding.plain.ui.theme.PlainTheme

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.extensions.timeAgo
import com.ismartcoding.plain.features.feed.FeedFetcher
import com.ismartcoding.plain.features.feed.FeedWorkerState
import com.ismartcoding.plain.features.feed.FeedWorkerStatus
import com.ismartcoding.plain.platform.formatDateTime
import com.ismartcoding.plain.platform.launchUrl
import com.ismartcoding.plain.ui.base.ActionButtons
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.CopyIconButton
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.IconTextDeleteButton
import com.ismartcoding.plain.ui.base.IconTextEditButton
import com.ismartcoding.plain.ui.base.IconTextSelectButton
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.PSwitch
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.Tips
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.FeedsViewModel
import com.ismartcoding.plain.ui.models.enterSelectMode
import com.ismartcoding.plain.ui.models.launchSafe
import com.ismartcoding.plain.ui.models.select
import com.ismartcoding.plain.ui.theme.green
import com.ismartcoding.plain.ui.theme.listItemValue
import com.ismartcoding.plain.ui.theme.red
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ViewFeedBottomSheet(
    feedsVM: FeedsViewModel,
    onDelete: (String) -> Unit = {},
) {
    val m = feedsVM.selectedItem.value ?: return
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        feedsVM.editFetchContent.value = m.fetchContent
    }

    val onDismiss = {
        feedsVM.selectedItem.value = null
    }

    PModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
    ) {
        VerticalSpace(32.dp)
        ActionButtons {
            IconTextSelectButton {
                feedsVM.enterSelectMode()
                feedsVM.select(m.id)
                onDismiss()
            }
            IconTextEditButton {
                feedsVM.showEditDialog(m)
                onDismiss()
            }
            IconTextDeleteButton {
                val id = m.id
                // Await the delete so the caller reloads the entry list after
                // the rows are actually gone, not mid-deletion.
                scope.launchSafe {
                    feedsVM.deleteAsync(setOf(id))
                    onDelete(id)
                    onDismiss()
                }
            }
        }
        VerticalSpace(dp = 24.dp)
        Subtitle(text = m.name)
        // Sync status card: state persisted by FeedFetcher on DFeed, refreshed
        // in place when a sync finishes while the sheet is open.
        val reason = feedSyncErrorReason(m.lastError.code)
        val syncBusy = FeedWorkerState.statusMap[m.id] == FeedWorkerStatus.PENDING
        PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
            PListItem(title = stringResource(Res.string.feeds_sync_last_synced), value = m.lastSyncAt?.timeAgo() ?: "—")
            PListItem(title = stringResource(Res.string.feeds_sync_result),
                subtitle = m.lastError.detail,
                action = {
                Text(
                    text = if (reason != null) stringResource(Res.string.feeds_sync_failed_with_reason, reason)
                    else stringResource(Res.string.feeds_sync_success),
                    style = MaterialTheme.typography.listItemValue(),
                    color = if (reason != null) MaterialTheme.colorScheme.red else MaterialTheme.colorScheme.green,
                )
            })
        }
        VerticalSpace(dp = 12.dp)
        POutlinedButton(
            text = if (syncBusy) stringResource(Res.string.syncing) else stringResource(Res.string.feeds_sync_now),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN),
            isLoading = syncBusy,
            enabled = !syncBusy,
            onClick = { scope.launch { FeedFetcher.fetchOne(m.id) } },
        )
        VerticalSpace(dp = 16.dp)
        PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
            PListItem(modifier = Modifier.clickable {
                try { launchUrl(m.url) } catch (_: Exception) { DialogHelper.showMessage(Res.string.no_browser_error) }
            }, title = m.url, separatedActions = true, action = {
                CopyIconButton(text = m.url, clipLabel = stringResource(Res.string.link))
            })
        }
        VerticalSpace(dp = 16.dp)
        PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
            PListItem(modifier = Modifier.clickable {
                feedsVM.editFetchContent.value = !feedsVM.editFetchContent.value
                m.fetchContent = feedsVM.editFetchContent.value
                feedsVM.updateFetchContent(m.id, feedsVM.editFetchContent.value)
            }, title = stringResource(Res.string.auto_fetch_full_content), action = {
                PSwitch(
                    activated = feedsVM.editFetchContent.value,
                ) {
                    feedsVM.editFetchContent.value = it
                    m.fetchContent = it
                    feedsVM.updateFetchContent(m.id, it)
                }
                HorizontalSpace(8.dp)
            })
        }
        Tips(text = stringResource(Res.string.auto_fetch_full_content_tips))
        VerticalSpace(dp = 16.dp)
        PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
            PListItem(title = stringResource(Res.string.created_at), value = m.createdAt.formatDateTime())
            PListItem(title = stringResource(Res.string.updated_at), value = m.updatedAt.formatDateTime())
        }
        BottomSpace()
    }
}
