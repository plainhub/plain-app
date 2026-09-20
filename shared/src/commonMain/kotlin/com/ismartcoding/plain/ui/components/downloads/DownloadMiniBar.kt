@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ismartcoding.plain.ui.components.downloads

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.features.download.DownloadStatus
import com.ismartcoding.plain.features.download.isTerminalDownloadStatus
import com.ismartcoding.plain.features.share.SharedFolderBatchTask
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.check
import com.ismartcoding.plain.i18n.download
import com.ismartcoding.plain.lib.extensions.formatBytes
import com.ismartcoding.plain.ui.theme.green
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * In-page floating summary of the batch tasks: the most relevant batch's
 * progress plus how many are running. Clicking opens the download list.
 * A finished batch flips to a green check state (no toast — the bar itself
 * is the completion signal).
 */
@Composable
fun DownloadMiniBar(
    tasks: List<SharedFolderBatchTask>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: DrawableResource = Res.drawable.download,
) {
    val task = mostRelevant(tasks) ?: return
    val running = tasks.count { !it.status.isTerminalDownloadStatus() }
    val failed = task.status == DownloadStatus.PARTIAL || task.status == DownloadStatus.FAILED
    val completed = task.status == DownloadStatus.COMPLETED
    val accent = when {
        failed -> MaterialTheme.colorScheme.error
        completed -> MaterialTheme.colorScheme.green
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 4.dp,
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(if (completed) Res.drawable.check else leadingIcon),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = miniBarTitle(task),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
                Text(
                    text = miniBarPct(task),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LinearProgressIndicator(
                progress = { task.fraction() },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                color = accent,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = miniBarDetail(task),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (running > 1) {
                    Text(
                        text = "$running",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

/** Running first, then queued, then attention-worthy failures, then newest. */
internal fun mostRelevant(tasks: List<SharedFolderBatchTask>): SharedFolderBatchTask? {
    if (tasks.isEmpty()) return null
    return tasks.lastOrNull { it.status == DownloadStatus.DOWNLOADING }
        ?: tasks.lastOrNull { it.status == DownloadStatus.PENDING }
        ?: tasks.lastOrNull {
            it.status == DownloadStatus.PARTIAL || it.status == DownloadStatus.FAILED
        }
        ?: tasks.last()
}

internal fun miniBarTitle(task: SharedFolderBatchTask): String =
    if (task.status == DownloadStatus.DOWNLOADING && task.totalFiles > 1 && task.currentFile.isNotEmpty()) {
        task.currentFile
    } else {
        task.title
    }

internal fun miniBarPct(task: SharedFolderBatchTask): String = when (task.status) {
    DownloadStatus.COMPLETED -> ""
    DownloadStatus.CANCELED -> ""
    DownloadStatus.PARTIAL, DownloadStatus.FAILED -> ""
    else -> "${(task.fraction() * 100).toInt()}%"
}

internal fun miniBarDetail(task: SharedFolderBatchTask): String {
    val bytes = "${task.downloadedSize.formatBytes()} / ${task.totalSize.formatBytes()}"
    return if (task.totalFiles > 1) "$bytes · ${task.doneFiles}/${task.totalFiles}" else bytes
}
