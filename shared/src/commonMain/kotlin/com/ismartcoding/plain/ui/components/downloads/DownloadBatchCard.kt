@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ismartcoding.plain.ui.components.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.features.download.DownloadCenter
import com.ismartcoding.plain.features.download.DownloadStatus
import com.ismartcoding.plain.features.download.isTerminalDownloadStatus
import com.ismartcoding.plain.features.share.SharedFolderBatchTask
import com.ismartcoding.plain.features.download.DownloadFailure
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.extensions.formatBytes
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import com.ismartcoding.plain.ui.theme.green
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * One aggregated batch card: title, destination, status chip, byte progress
 * with the current file, and an expandable per-file failure list with retry.
 */
@Composable
fun DownloadBatchCard(
    task: SharedFolderBatchTask,
    modifier: Modifier = Modifier,
) {
    var failuresExpanded by remember(task.id) { mutableStateOf(false) }
    val active = !task.status.isTerminalDownloadStatus()
    Surface(
        modifier = modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.cardBackgroundNormal,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(batchIcon(task)),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = batchSubtitle(task),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (active) {
                    PIconButton(
                        icon = Res.drawable.x,
                        contentDescription = stringResource(Res.string.cancel),
                    ) { DownloadCenter.cancel(task.id) }
                } else {
                    if (task.status == DownloadStatus.PARTIAL || task.status == DownloadStatus.FAILED) {
                        PIconButton(
                            icon = Res.drawable.circle_alert,
                            contentDescription = stringResource(Res.string.try_again),
                        ) {
                            failuresExpanded = false
                            com.ismartcoding.plain.features.share.SharedFolderDownloadEngine.retryFailed(task.id)
                        }
                    }
                    PIconButton(
                        icon = Res.drawable.x,
                        contentDescription = stringResource(Res.string.cancel),
                    ) { DownloadCenter.remove(task.id) }
                }
            }

            StatusChip(task.status, Modifier.padding(top = 8.dp))

            // Whole-batch errors (engine failures, zip errors) never enter the
            // per-file failures list — surface them here or they vanish.
            if (task.status == DownloadStatus.FAILED && task.error.isNotEmpty()) {
                StatusLine(task.error, color = MaterialTheme.colorScheme.error)
            }

            if (active && task.downloadedSize > 0) {
                LinearProgressIndicator(
                    progress = { task.fraction() },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text(
                        text = progressLine(task),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${(task.fraction() * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            when {
                task.status == DownloadStatus.PENDING && task.totalFiles == 0 ->
                    StatusLine(stringResource(Res.string.reading_folder))
                task.status == DownloadStatus.PENDING ->
                    StatusLine(stringResource(Res.string.queued))
                task.packing ->
                    StatusLine(stringResource(Res.string.packing))
                task.status == DownloadStatus.DOWNLOADING && task.totalFiles > 1 && task.currentFile.isNotEmpty() ->
                    StatusLine(
                        stringResource(
                            Res.string.downloading_file,
                            task.currentFile,
                            (task.doneFiles + 1).coerceAtMost(task.totalFiles),
                            task.totalFiles,
                        ),
                    )
            }

            if (task.status == DownloadStatus.COMPLETED) {
                StatusLine(doneLine(task), color = MaterialTheme.colorScheme.green)
            }
            if (task.status == DownloadStatus.CANCELED && task.doneFiles > 0) {
                StatusLine(
                    stringResource(Res.string.canceled) + " · " +
                        pluralStringResource(Res.plurals.kept_n_files, task.doneFiles),
                )
            }

            if (task.failures.isNotEmpty()) {
                FailureSummary(
                    failures = task.failures,
                    expanded = failuresExpanded,
                    onToggle = { failuresExpanded = !failuresExpanded },
                    onRetryItem = {
                        failuresExpanded = false
                        com.ismartcoding.plain.features.share.SharedFolderDownloadEngine.retryFailed(task.id)
                    },
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun StatusLine(text: String, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun StatusChip(status: DownloadStatus, modifier: Modifier = Modifier) {
    val (labelRes, container, content) = when (status) {
        DownloadStatus.DOWNLOADING -> Triple(Res.string.in_progress, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        DownloadStatus.PENDING -> Triple(Res.string.queued, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
        DownloadStatus.COMPLETED -> Triple(Res.string.completed, MaterialTheme.colorScheme.green.copy(alpha = 0.12f), MaterialTheme.colorScheme.green)
        DownloadStatus.PARTIAL -> Triple(Res.string.partial_failure, MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        DownloadStatus.FAILED -> Triple(Res.string.failed, MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        DownloadStatus.CANCELED -> Triple(Res.string.canceled, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
        DownloadStatus.PAUSED -> Triple(Res.string.queued, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Surface(shape = RoundedCornerShape(8.dp), color = container, modifier = modifier) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = content,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun FailureSummary(
    failures: List<DownloadFailure>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onRetryItem: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val first = failures.first()
    Column(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.fillMaxWidth(),
            onClick = onToggle,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = first.path.substringAfterLast('/') + " · " + first.error +
                        if (failures.size > 1) " (+${failures.size - 1})" else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (expanded) "▲" else "▼",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
        if (expanded) {
            Spacer(Modifier.size(4.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    // Failures render on demand only: a 1000-file batch with
                    // 3 failures still composes at most its expanded rows.
                    failures.forEach { failure ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = failure.path.substringAfterLast('/'),
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = failure.error,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            PIconButton(
                                icon = Res.drawable.circle_alert,
                                contentDescription = stringResource(Res.string.try_again),
                            ) { onRetryItem() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun batchIcon(task: SharedFolderBatchTask) = when {
    task.zipName.isNotEmpty() || task.type == com.ismartcoding.plain.features.share.ShareBatchType.ZIP -> Res.drawable.folder
    else -> Res.drawable.download
}

@Composable
private fun batchSubtitle(task: SharedFolderBatchTask): String {
    val target = task.targetDir.ifEmpty { "Downloads/PlainApp" }
    val files = if (task.totalFiles > 0) {
        pluralStringResource(Res.plurals.items, task.totalFiles) + " · " + task.totalSize.formatBytes()
    } else {
        task.entries.size.toString() + " · …"
    }
    return "$target · $files"
}

@Composable
private fun progressLine(task: SharedFolderBatchTask): String {
    val bytes = task.downloadedSize.formatBytes() + " / " + task.totalSize.formatBytes()
    return if (task.downloadSpeed > 0) "$bytes · ${task.downloadSpeed.formatBytes()}/s" else bytes
}

@Composable
private fun doneLine(task: SharedFolderBatchTask): String =
    stringResource(Res.string.saved_to_x, task.targetDir.ifEmpty { "Downloads/PlainApp" })
