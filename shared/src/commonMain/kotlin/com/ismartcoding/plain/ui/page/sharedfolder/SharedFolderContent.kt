package com.ismartcoding.plain.ui.page.sharedfolder

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.features.share.SharedLink
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.BottomActionButtons
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.models.BreadcrumbItem
import com.ismartcoding.plain.ui.page.files.components.BreadcrumbView
import org.jetbrains.compose.resources.stringResource

/** Page body: meta banner, breadcrumbs, sync status and the state-switched entry list. */
@Composable
internal fun SharedFolderContent(
    state: SharedFolderState,
    contentPadding: PaddingValues,
) {
    val active = state.activeLink

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        MetaBanner(state.rootInfo)
        if (state.pathError) {
            ErrorBanner(onRetry = { state.retry() })
        }
        val breadcrumbs = buildList {
            add(BreadcrumbItem(state.rootInfo?.name ?: "/", ""))
            state.crumbs.forEach { add(BreadcrumbItem(it.name, it.virtualPath)) }
        }
        BreadcrumbView(
            breadcrumbs = breadcrumbs,
            selectedIndex = breadcrumbs.lastIndex,
            onItemClick = { item -> state.onCrumbClick(item.path) },
        )
        state.syncStatus?.let { SyncStatusRow(it) }
        when {
            state.shareMsg == null -> CenteredHint(stringResource(Res.string.cannot_load_share))
            state.currentInfo == null && state.pathLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            state.currentInfo == null && state.pathError -> CenteredHint(stringResource(Res.string.cannot_load_share))
            state.entries.isEmpty() && state.currentInfo != null -> CenteredHint(stringResource(Res.string.shared_folder_empty))
            state.entries.isNotEmpty() && active != null -> EntryList(state, active, Modifier.weight(1f))
        }
    }
}

@Composable
private fun EntryList(
    state: SharedFolderState,
    link: SharedLink,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (!state.selectMode) {
            Text(
                text = stringResource(Res.string.select_items_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(state.entries, key = { it.virtualPath }) { entry ->
                EntryRow(
                    entry = entry,
                    link = link,
                    urlToken = state.rootInfo?.urlToken ?: "",
                    selectMode = state.selectMode,
                    selected = state.selected.contains(entry),
                    progress = state.progressOf(entry),
                    previewLoading = state.isPreviewLoading(entry),
                    onClick = { state.onEntryClick(entry) },
                    onLongClick = { state.onEntryLongClick(entry) },
                    onDownload = { state.downloadTarget = entry },
                )
            }
        }
        if (state.selectMode) {
            BottomActionButtons {
                PFilledButton(
                    text = stringResource(Res.string.save_selected, state.selected.size),
                    onClick = { state.showSaveSelectedSheet = true },
                    enabled = state.selected.isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun SyncStatusRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun CenteredHint(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
