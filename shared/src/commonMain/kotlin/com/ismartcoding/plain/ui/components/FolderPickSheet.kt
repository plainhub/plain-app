@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ismartcoding.plain.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.getInternalStoragePath
import com.ismartcoding.plain.platform.listFilesInDir
import com.ismartcoding.plain.ui.base.BottomActionButtons
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.components.DirBrowserRow
import org.jetbrains.compose.resources.stringResource

/**
 * Bottom sheet to pick a destination folder on the device filesystem.
 * Confirms the folder currently being viewed.
 */
@Composable
fun FolderPickSheet(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val rootPath = remember { getInternalStoragePath() }
    var currentPath by remember { mutableStateOf(rootPath) }
    var dirs by remember { mutableStateOf<List<DFile>>(emptyList()) }

    LaunchedEffect(currentPath) {
        withIO {
            dirs = listFilesInDir(currentPath, showHidden = false, sortBy = FileSortBy.NAME_ASC).filter { it.isDir }
        }
    }

    PModalBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.fillMaxWidth()) {
        PBottomSheetTopAppBar(
            title = stringResource(Res.string.pick_directory),
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
                    DirBrowserRow(
                        name = "..",
                        subtitle = stringResource(Res.string.back),
                        onClick = { currentPath = currentPath.substringBeforeLast('/', missingDelimiterValue = rootPath).ifEmpty { rootPath } },
                    )
                }
            }
            items(dirs, key = { it.path }) { dir ->
                DirBrowserRow(
                    name = dir.name,
                    onClick = { currentPath = dir.path },
                )
            }
        }
        BottomActionButtons {
            PFilledButton(
                text = stringResource(Res.string.choose_this_folder),
                onClick = { onConfirm(currentPath) },
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
            )
        }
    }
}
