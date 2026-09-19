@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ismartcoding.plain.ui.components.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.features.download.DownloadCenter
import com.ismartcoding.plain.features.download.isTerminalDownloadStatus
import com.ismartcoding.plain.features.share.SharedFolderBatchTask
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.helpers.confirmActionAsync
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Global download list: running and finished share batches behind two filter
 * pills. Shared by the folder page and the app-level floating widget; reads
 * straight from [DownloadCenter], so entries survive page navigation.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DownloadListSheet(
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val tasksMap = DownloadCenter.progress.collectAsState().value
    val shareTasks = tasksMap.values.filterIsInstance<SharedFolderBatchTask>()
    val running = shareTasks.filter { !it.status.isTerminalDownloadStatus() }
    val finished = shareTasks.filter { it.status.isTerminalDownloadStatus() }
    var runningTab by remember { mutableStateOf(true) }
    val visible = if (runningTab) running else finished

    PModalBottomSheet(onDismissRequest = onDismiss) {
        Column {
            PBottomSheetTopAppBar(
                title = stringResource(Res.string.downloads),
                actions = {
                    if (!runningTab && finished.isNotEmpty()) {
                        PTextButton(text = stringResource(Res.string.clear_completed), onClick = {
                            scope.launch {
                                confirmActionAsync(
                                    title = Res.string.clear,
                                    message = Res.string.clear_completed_confirm,
                                    callback = { finished.forEach { DownloadCenter.remove(it.id) } },
                                    danger = true,
                                )
                            }
                        })
                    }
                },
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterPill(
                    label = stringResource(Res.string.in_progress) + if (running.isNotEmpty()) " (${running.size})" else "",
                    selected = runningTab,
                    onClick = { runningTab = true },
                )
                FilterPill(
                    label = stringResource(Res.string.completed) + if (finished.isNotEmpty()) " (${finished.size})" else "",
                    selected = !runningTab,
                    onClick = { runningTab = false },
                )
            }
            if (visible.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(if (runningTab) Res.string.no_running_downloads else Res.string.no_finished_downloads),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp,
                    ),
                ) {
                    items(visible, key = { it.id }) { task ->
                        DownloadBatchCard(task)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.cardBackgroundNormal,
        modifier = Modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}
