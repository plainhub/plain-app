package com.ismartcoding.plain.ui.page.feeds

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.extensions.getFinalPath
import com.ismartcoding.plain.extensions.timeAgo
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.CheckCircle
import com.ismartcoding.plain.ui.models.FeedEntriesViewModel
import com.ismartcoding.plain.ui.models.select
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTag
import com.ismartcoding.plain.ui.theme.listItemTitle
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Title style shared by entry rows and digest previews: big bold when unread, muted regular when read. */
@Composable
private fun entryTitleStyle(read: Boolean) = if (read) {
    MaterialTheme.typography.titleMedium.copy(
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
} else {
    MaterialTheme.typography.listItemTitle()
}

/**
 * Sticky day separator for the grouped feed list. The mark-read action only
 * covers the entries loaded in the current paging window.
 */
@Composable
internal fun FeedDayHeaderRow(
    row: FeedListRow.DayHeader,
    onMarkRead: () -> Unit,
) {
    val label = when (row.kind) {
        DayKind.TODAY -> stringResource(Res.string.today)
        DayKind.YESTERDAY -> stringResource(Res.string.yesterday)
        DayKind.DATE -> row.dateLabel
    }
    val meta = listOf(
        stringResource(Res.string.n_articles, row.count),
        if (row.unreadCount > 0) stringResource(Res.string.n_unread, row.unreadCount) else stringResource(Res.string.all_read),
    ).joinToString(" · ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
        HorizontalSpace(dp = 8.dp)
        Text(
            meta,
            style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.weight(1f))
        if (row.unreadCount > 0) {
            TextButton(onClick = onMarkRead) {
                Text(stringResource(Res.string.mark_all_read), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/**
 * Per-feed cluster header, borderless and full-bleed: letter chip + name +
 * counts. Collapse is expressed by the rotating chevron plus child rows
 * indented to align under the feed name (16 margin + 32 chip + 8 gap = 56dp).
 */
@Composable
internal fun FeedClusterHeaderRow(
    row: FeedListRow.ClusterHeader,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val logo = row.feed?.logo
        if (!logo.isNullOrEmpty()) {
            AsyncImage(
                model = logo.getFinalPath(),
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentDescription = row.feed?.name,
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = row.feed?.name?.take(1)?.uppercase() ?: "#",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
        }
        HorizontalSpace(dp = 8.dp)
        Text(
            text = row.feed?.name ?: "#",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        HorizontalSpace(dp = 8.dp)
        Text(
            text = stringResource(Res.string.n_articles, row.count),
            style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            maxLines = 1,
        )
        if (row.unreadCount > 0) {
            HorizontalSpace(dp = 8.dp)
            Text(
                text = stringResource(Res.string.n_unread, row.unreadCount),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold,
                ),
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        HorizontalSpace(dp = 8.dp)
        Icon(
            painter = painterResource(Res.drawable.chevron_right),
            contentDescription = null,
            modifier = Modifier
                .size(20.dp)
                .rotate(if (row.collapsed) 0f else 90f),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * One entry as a full-bleed borderless row, indented under its cluster's feed
 * name. Unread state is typography only: bold dark title vs muted regular.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FeedClusterEntryRow(
    row: FeedListRow.Entry,
    feedEntriesVM: FeedEntriesViewModel,
    tags: List<DTag>,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onClickTag: (DTag) -> Unit,
) {
    val m = row.entry
    val unread = !m.read
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 56.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
    ) {
        if (feedEntriesVM.selectMode.value) {
            CheckCircle(
                selected = feedEntriesVM.selectedIds.contains(m.id),
                onClick = { feedEntriesVM.select(m.id) },
            )
            HorizontalSpace(dp = 8.dp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = m.title,
                style = entryTitleStyle(read = !unread),
                maxLines = 2,
            )
            // Same labelLarge metrics for time and tags; CenterVertically on the
            // line because FlowRow top-aligns items and CJK tag glyphs measure a
            // taller line box, which made the time look shifted up.
            FlowRow(
                modifier = Modifier.padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // The cluster header already names the feed; byline keeps author + time.
                Text(
                    text = arrayOf(m.author, m.publishedAt.timeAgo()).filter { it.isNotEmpty() }.joinToString(" · "),
                    modifier = Modifier.align(Alignment.CenterVertically),
                    style = MaterialTheme.typography.listItemSubtitle(),
                )
                tags.forEach { tag ->
                    Text(
                        text = "#" + tag.name,
                        modifier = Modifier
                            .wrapContentHeight()
                            .align(Alignment.CenterVertically)
                            .clickable { onClickTag(tag) },
                        style = MaterialTheme.typography.listItemTag(),
                    )
                }
            }
        }
        if (m.image.isNotEmpty()) {
            HorizontalSpace(dp = 12.dp)
            AsyncImage(
                model = m.image.getFinalPath(),
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentDescription = m.image,
                contentScale = ContentScale.Crop,
            )
        }
    }
}

/**
 * Collapsed cluster body: newest entry titles as plain indented text lines,
 * full-bleed and tappable to expand. Unread titles bold, read titles muted.
 */
@Composable
internal fun FeedClusterDigestRow(
    row: FeedListRow.CollapsedDigest,
    onToggle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle)
            .padding(start = 56.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
    ) {
        row.entries.forEachIndexed { index, entry ->
            if (index > 0) VerticalSpace(dp = 4.dp)
            Text(
                text = entry.title,
                style = entryTitleStyle(read = entry.read),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
