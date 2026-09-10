package com.ismartcoding.plain.ui.page.feeds

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.db.DFeed
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.extensions.timeAgo
import com.ismartcoding.plain.features.feed.FeedSyncErrorCode
import com.ismartcoding.plain.features.feed.FeedWorkerState
import com.ismartcoding.plain.features.feed.FeedWorkerStatus
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.setClipboardText
import com.ismartcoding.plain.ui.base.PAlert
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.AlertType
import com.ismartcoding.plain.ui.theme.red
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Maps the persisted DFeed.lastError.code to a localized user-facing reason. */
@Composable
internal fun feedSyncErrorReason(code: String): String? {
    val errorCode = FeedSyncErrorCode.fromString(code) ?: return null
    if (errorCode == FeedSyncErrorCode.NONE) return null
    return stringResource(
        when (errorCode) {
            FeedSyncErrorCode.NO_NETWORK -> Res.string.feed_err_no_network
            FeedSyncErrorCode.DNS -> Res.string.feed_err_dns
            FeedSyncErrorCode.TIMEOUT -> Res.string.feed_err_timeout
            FeedSyncErrorCode.SERVER -> Res.string.feed_err_server
            FeedSyncErrorCode.SSL -> Res.string.feed_err_ssl
            FeedSyncErrorCode.PARSE -> Res.string.feed_err_parse
            FeedSyncErrorCode.UNKNOWN, FeedSyncErrorCode.NONE -> Res.string.sync_failed
        },
    )
}

/**
 * State-driven sync failure banner for the feeds page, built on PAlert:
 * collapsed headline, expandable per-feed rows with classified reasons,
 * inline retry and a copyable raw error. Replaces the old error dialogs.
 */
@Composable
internal fun FeedSyncBanner(
    failedFeeds: List<DFeed>,
    singleFeed: Boolean,
    onRetry: (String) -> Unit,
    onRetryAll: () -> Unit,
) {
    // The key includes each feed's last_sync_at, so a new sync that fails
    // again produces a new key and the dismissal is forgotten (a dismissed
    // banner must still come back on the next failed sync attempt).
    val dismissedKey = failedFeeds.joinToString("|") { "${it.id}:${it.lastSyncAt?.toEpochMilliseconds() ?: 0L}" }
    var dismissed by remember(dismissedKey) { mutableStateOf(false) }
    if (dismissed) return
    var expanded by remember { mutableStateOf(false) }
    var detailFeedId by remember { mutableStateOf<String?>(null) }

    val headline = if (singleFeed) {
        stringResource(Res.string.feeds_sync_failed_single, feedSyncErrorReason(failedFeeds.first().lastError.code) ?: "")
    } else {
        stringResource(Res.string.feeds_sync_failed_n, failedFeeds.size)
    }

    VerticalSpace(dp = 8.dp)
    PAlert(
        description = headline,
        type = AlertType.ERROR,
        onClick = { expanded = !expanded },
        trailing = {
            Icon(
                painter = painterResource(Res.drawable.chevron_up),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(if (expanded) 0f else 180f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(Res.drawable.x),
                contentDescription = stringResource(Res.string.close),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { dismissed = true }
                    .padding(6.dp)
                    .size(20.dp),
            )
        },
        content = {
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column {
                    // Flat divider list on the tonal container — no nested cards.
                    failedFeeds.forEachIndexed { index, feed ->
                        if (index > 0) HorizontalDivider()
                        FeedSyncBannerRow(
                            feed = feed,
                            detailExpanded = detailFeedId == feed.id,
                            onToggleDetail = { detailFeedId = if (detailFeedId == feed.id) null else feed.id },
                            onRetry = { onRetry(feed.id) },
                        )
                    }
                    if (!singleFeed && failedFeeds.size > 1) {
                        HorizontalDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
                        ) {
                            PTextButton(
                                text = stringResource(Res.string.feeds_sync_retry_all),
                                buttonSize = ButtonSize.SMALL,
                                onClick = onRetryAll,
                            )
                        }
                    }
                }
            }
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeedSyncBannerRow(
    feed: DFeed,
    detailExpanded: Boolean,
    onToggleDetail: () -> Unit,
    onRetry: () -> Unit,
) {
    val busy = FeedWorkerState.statusMap[feed.id] == FeedWorkerStatus.PENDING
    val reason = feedSyncErrorReason(feed.lastError.code) ?: stringResource(Res.string.sync_failed)
    val lastSyncedAt = feed.lastSyncAt?.timeAgo()
    var showMenu by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onToggleDetail,
                // Long-press replaces the old copy icon button: the raw error
                // is reachable without spending permanent row space.
                onLongClick = { showMenu = true },
            )
            .padding(vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = feed.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (lastSyncedAt != null) "$reason · $lastSyncedAt" else reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.red,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            PTextButton(
                text = if (busy) stringResource(Res.string.syncing) else stringResource(Res.string.feeds_sync_retry),
                buttonSize = ButtonSize.SMALL,
                isLoading = busy,
                enabled = !busy,
                onClick = onRetry,
            )
        }
        AnimatedVisibility(visible = detailExpanded) {
            Text(
                text = feed.lastError.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            val copyLabel = stringResource(Res.string.copy)
            DropdownMenuItem(
                text = { Text(copyLabel) },
                onClick = {
                    setClipboardText(copyLabel, feed.lastError.detail)
                    showMenu = false
                },
            )
        }
    }
}
