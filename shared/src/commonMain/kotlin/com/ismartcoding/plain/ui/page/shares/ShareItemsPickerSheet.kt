@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ismartcoding.plain.ui.page.shares

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.extensions.formatBytes
import com.ismartcoding.plain.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.getInternalStoragePath
import com.ismartcoding.plain.platform.listFilesInDir
import com.ismartcoding.plain.ui.base.BottomActionButtons
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Bottom sheet to multi-select real files and folders from device storage.
 * Directory rows enter on tap and toggle selection via the trailing checkbox;
 * file rows toggle on tap anywhere. Confirm returns every selected real path.
 */
@Composable
fun ShareItemsPickerSheet(
    initialSelected: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
) {
    val rootPath = remember { getInternalStoragePath() }
    var currentPath by remember { mutableStateOf(rootPath) }
    var entries by remember { mutableStateOf<List<DFile>>(emptyList()) }
    val selected = remember { mutableStateListOf<String>().apply { addAll(initialSelected) } }

    LaunchedEffect(currentPath) {
        withIO {
            entries = listFilesInDir(currentPath, showHidden = false, sortBy = FileSortBy.NAME_ASC)
        }
    }

    fun toggle(path: String) {
        if (selected.contains(path)) selected.remove(path) else selected.add(path)
    }

    PModalBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.fillMaxWidth()) {
        PBottomSheetTopAppBar(
            title = stringResource(Res.string.add_items),
            navigationIcon = {
                PIconButton(
                    icon = Res.drawable.x,
                    contentDescription = stringResource(Res.string.close),
                    tint = MaterialTheme.colorScheme.onSurface,
                ) { onDismiss() }
            },
        )
        Text(
            text = currentPath,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        ) {
            if (currentPath != rootPath) {
                item {
                    PickerRow(
                        name = "..",
                        subtitle = stringResource(Res.string.back),
                        isDir = true,
                        selected = false,
                        showCheckbox = false,
                        onClick = { currentPath = currentPath.substringBeforeLast('/', missingDelimiterValue = rootPath).ifEmpty { rootPath } },
                        onToggle = {},
                    )
                }
            }
            items(entries, key = { it.path }) { entry ->
                PickerRow(
                    name = entry.name,
                    subtitle = if (entry.isDir) "" else entry.size.formatBytes(),
                    isDir = entry.isDir,
                    selected = selected.contains(entry.path),
                    onClick = {
                        if (entry.isDir) currentPath = entry.path else toggle(entry.path)
                    },
                    onToggle = { toggle(entry.path) },
                )
            }
        }
        BottomActionButtons {
            PFilledButton(
                text = stringResource(Res.string.done) + " (${selected.size})",
                onClick = { onConfirm(selected.toSet()) },
                enabled = selected.isNotEmpty(),
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun PickerRow(
    name: String,
    subtitle: String,
    isDir: Boolean,
    selected: Boolean,
    showCheckbox: Boolean = true,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(if (isDir) Res.drawable.folder else Res.drawable.file_text),
            contentDescription = name,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (showCheckbox) {
            Checkbox(checked = selected, onCheckedChange = { onToggle(it) })
        }
    }
}
