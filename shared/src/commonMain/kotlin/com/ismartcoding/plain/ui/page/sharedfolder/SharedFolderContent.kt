package com.ismartcoding.plain.ui.page.sharedfolder

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.features.share.SharedFileDto
import com.ismartcoding.plain.features.share.SharedFolderBatchTask
import com.ismartcoding.plain.features.share.SharedLink
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.BottomActionButtons
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.components.downloads.DownloadMiniBar
import com.ismartcoding.plain.ui.models.BreadcrumbItem
import com.ismartcoding.plain.ui.page.files.components.BreadcrumbView
import org.jetbrains.compose.resources.stringResource

/**
 * Page body: breadcrumbs, the state-switched entry list and the floating
 * download mini bar (which reserves list clearance below itself).
 */
@Composable
internal fun SharedFolderContent(
    state: SharedFolderState,
    contentPadding: PaddingValues,
    tasks: List<SharedFolderBatchTask>,
    onOpenDownloads: () -> Unit,
) {
    val active = state.activeLink

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
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
        val showMini = tasks.isNotEmpty() && !state.selectMode
        var miniClearance by remember { mutableIntStateOf(0) }
        Box(modifier = Modifier.weight(1f)) {
            when {
                state.shareMsg == null -> CenteredHint(stringResource(Res.string.cannot_load_share))
                state.currentInfo == null && state.pathLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
                state.currentInfo == null && state.pathError -> CenteredHint(stringResource(Res.string.cannot_load_share))
                state.entries.isEmpty() && state.currentInfo != null -> CenteredHint(stringResource(Res.string.shared_folder_empty))
                state.entries.isNotEmpty() && active != null -> EntryList(
                    state = state,
                    link = active,
                    bottomPadding = if (showMini) miniClearance + 16 else 0,
                )
            }
            if (showMini) {
                DownloadMiniBar(
                    tasks = tasks,
                    onClick = onOpenDownloads,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .onGloballyPositioned { miniClearance = it.size.height },
                )
            }
        }
    }
}

@Composable
private fun EntryList(
    state: SharedFolderState,
    link: SharedLink,
    bottomPadding: Int,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPadding.dp),
    ) {
        items(state.entries, key = { it.virtualPath }) { entry ->
            EntryRow(
                entry = entry,
                link = link,
                urlToken = state.rootInfo?.urlToken ?: "",
                selectMode = state.selectMode,
                selected = state.selected.contains(entry),
                busy = state.isEntryBusy(entry),
                previewLoading = state.isPreviewLoading(entry),
                onClick = { state.onEntryClick(entry) },
                onLongClick = { state.onEntryLongClick(entry) },
                onDownload = { state.downloadTarget = entry },
            )
        }
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
