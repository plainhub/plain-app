package com.ismartcoding.plain.ui.page.feeds
import androidx.compose.foundation.layout.padding
import com.ismartcoding.plain.ui.theme.PlainTheme

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.db.DTagRelation
import com.ismartcoding.plain.platform.formatDateTime
import com.ismartcoding.plain.platform.launchUrl
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.CopyIconButton
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.PSheetHeader
import com.ismartcoding.plain.ui.base.PSheetHeaderThumb
import com.ismartcoding.plain.ui.base.PSheetPrimaryAction
import com.ismartcoding.plain.ui.base.PSheetPrimaryDeleteAction
import com.ismartcoding.plain.ui.base.PSheetPrimaryActionsRow
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.TagSelector
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.FeedEntriesViewModel
import com.ismartcoding.plain.ui.models.FeedsViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.enterSelectMode
import com.ismartcoding.plain.ui.models.select

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ViewFeedEntryBottomSheet(
    feedEntriesVM: FeedEntriesViewModel,
    feedsVM: FeedsViewModel,
    tagsVM: TagsViewModel,
    tagsMap: Map<String, List<DTagRelation>>,
    tagsState: List<DTag>,
) {
    val m = feedEntriesVM.selectedItem.value ?: return
    val feeds by feedsVM.itemsFlow.collectAsState()
    val feedLogo = feeds.find { it.id == m.feedId }?.logo?.takeIf { it.isNotEmpty() }
    val scope = rememberCoroutineScope()
    val onDismiss = {
        feedEntriesVM.selectedItem.value = null
    }

    PModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
    ) {
        LazyColumn {
            item {
                VerticalSpace(16.dp)
            }
            item {
                PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                    PSheetHeader(
                        thumbnail = {
                            PSheetHeaderThumb(model = feedLogo, fallbackIcon = Res.drawable.rss)
                        },
                        title = m.title.ifEmpty { m.url },
                        subtitle = (if (m.author.isNotEmpty()) m.author + " · " else "") + m.publishedAt.formatDateTime(),
                    )
                    PSheetPrimaryActionsRow {
                        if (!feedEntriesVM.showSearchBar.value) {
                            PSheetPrimaryAction(Res.drawable.list_checks, stringResource(Res.string.select)) {
                                feedEntriesVM.enterSelectMode()
                                feedEntriesVM.select(m.id)
                                onDismiss()
                            }
                        }
                        // Toggle in place: the sheet stays open and the action flips
                        // to the opposite state via the updated selected copy.
                        PSheetPrimaryAction(
                            if (m.read) Res.drawable.circle_dot else Res.drawable.circle_check,
                            stringResource(if (m.read) Res.string.mark_as_unread else Res.string.mark_as_read),
                        ) {
                            feedEntriesVM.selectedItem.value = m.copy(read = !m.read)
                            feedEntriesVM.markRead(setOf(m.id), !m.read)
                        }
                        PSheetPrimaryDeleteAction(stringResource(Res.string.delete)) {
                            feedEntriesVM.delete(tagsVM, setOf(m.id))
                            onDismiss()
                        }
                    }
                }
                VerticalSpace(dp = 16.dp)
                Subtitle(text = stringResource(Res.string.tags))
                TagSelector(
                    data = m,
                    tagsVM = tagsVM,
                    tagsMap = tagsMap,
                    tagsState = tagsState,
                    onChangedAsync = {
                        feedEntriesVM.loadAsync(tagsVM)
                    }
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
                    PListItem(title = stringResource(Res.string.published_at), value = m.publishedAt.formatDateTime())
                    PListItem(title = stringResource(Res.string.created_at), value = m.createdAt.formatDateTime())
                    PListItem(title = stringResource(Res.string.updated_at), value = m.updatedAt.formatDateTime())
                }
                BottomSpace()
            }
        }
    }
}
