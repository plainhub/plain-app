package com.ismartcoding.plain.ui.page.feeds

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.db.DFeed
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.enums.FeedEntryFilterType
import com.ismartcoding.plain.extensions.getFinalPath
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.SidebarItem
import com.ismartcoding.plain.ui.components.SidebarSectionHeader
import com.ismartcoding.plain.ui.models.FeedEntriesViewModel
import com.ismartcoding.plain.ui.models.FeedsViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Drawer content for the feed entries page: All, Today, Feeds (with a "+"
 * action to add a feed; long-press manages a feed), then Tags.
 */
@Composable
internal fun FeedEntriesDrawerContent(
    feedEntriesVM: FeedEntriesViewModel,
    feedsVM: FeedsViewModel,
    feedsState: List<DFeed>,
    tagsState: List<DTag>,
    drawerState: DrawerState,
    onSelect: (feedId: String, filterType: FeedEntryFilterType, tag: DTag?) -> Unit,
    onOpenCatalog: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var feedsExpanded by remember { mutableStateOf(true) }
    var tagsExpanded by remember { mutableStateOf(true) }
    val closeDrawer: () -> Unit = { scope.launch { drawerState.close() } }

    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
            .padding(NavigationDrawerItemDefaults.ItemPadding)
    ) {
        VerticalSpace(dp = 16.dp)

        SidebarItem(
            label = stringResource(Res.string.all),
            icon = Res.drawable.layout_grid,
            isSelected = feedEntriesVM.feedId.value.isEmpty() && feedEntriesVM.tag.value == null && feedEntriesVM.filterType.value == FeedEntryFilterType.DEFAULT,
            onClick = { onSelect("", FeedEntryFilterType.DEFAULT, null) },
            badge = feedEntriesVM.total.intValue.toString()
        )

        SidebarItem(
            label = stringResource(Res.string.today),
            icon = Res.drawable.history,
            isSelected = feedEntriesVM.feedId.value.isEmpty() && feedEntriesVM.tag.value == null && feedEntriesVM.filterType.value == FeedEntryFilterType.TODAY,
            onClick = { onSelect("", FeedEntryFilterType.TODAY, null) },
            badge = feedEntriesVM.totalToday.value.toString()
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        SidebarSectionHeader(
            title = stringResource(Res.string.feeds),
            isExpanded = feedsExpanded,
            onToggle = { feedsExpanded = !feedsExpanded },
            onAction = onOpenCatalog,
            actionIcon = Res.drawable.plus
        )
        if (feedsExpanded) {
            feedsState.forEach { feed ->
                SidebarItem(
                    label = feed.name,
                    // Same identity as the list's cluster header: synced logo, or a
                    // first-letter chip on primaryContainer until one exists.
                    leading = {
                        if (feed.logo.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = feed.name.take(1).uppercase(),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                )
                            }
                        } else {
                            AsyncImage(
                                model = feed.logo.getFinalPath(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    },
                    isSelected = feedEntriesVM.feedId.value == feed.id,
                    onClick = { onSelect(feed.id, FeedEntryFilterType.DEFAULT, null) },
                    onLongClick = { feedsVM.selectedItem.value = feed },
                    badge = feed.entryCount.toString(),
                    // Persisted sync failure: keep the broken feed visible at a glance.
                    showErrorDot = feed.hasSyncError,
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        SidebarSectionHeader(
            title = stringResource(Res.string.tags),
            isExpanded = tagsExpanded,
            onToggle = { tagsExpanded = !tagsExpanded },
            onAction = { feedEntriesVM.showTagsDialog.value = true; closeDrawer() },
            actionIcon = Res.drawable.plus
        )
        if (tagsExpanded) {
            tagsState.forEach { tag ->
                SidebarItem(
                    label = tag.name,
                    icon = Res.drawable.tag,
                    isSelected = feedEntriesVM.feedId.value.isEmpty() && feedEntriesVM.tag.value?.id == tag.id,
                    onClick = { onSelect("", FeedEntryFilterType.DEFAULT, tag) },
                    badge = tag.count.toString()
                )
            }
        }
        BottomSpace()
    }
}
