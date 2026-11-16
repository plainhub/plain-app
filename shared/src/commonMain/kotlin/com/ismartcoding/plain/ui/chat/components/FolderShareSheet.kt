package com.ismartcoding.plain.ui.chat.components

import com.ismartcoding.plain.models.File as FileEntry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.features.files.FileSortBy
import com.ismartcoding.plain.features.shares.ShareExpiry
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.extensions.formatBytes
import com.ismartcoding.plain.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.platform.files.getInternalStorageName
import com.ismartcoding.plain.platform.files.getInternalStoragePath
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.ui.base.AlertType
import com.ismartcoding.plain.ui.base.BottomActionButtons
import com.ismartcoding.plain.ui.base.PAlert
import com.ismartcoding.plain.ui.components.ClipboardTextField
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PFilterChip
import com.ismartcoding.plain.ui.base.PFilledButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.components.DirBrowserRow
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.files.label
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.CancellationException

/**
 * Bottom sheet to share a whole folder into the current chat: browse the
 * device storage, pick a folder (the folder being viewed), choose an expiry,
 * then create the share link and send a folder card message.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FolderShareSheet(
    onDismiss: () -> Unit,
    onConfirm: (dirPath: String, name: String, expiry: ShareExpiry) -> Unit,
) {
    val source = com.ismartcoding.plain.ui.navigation.LocalContentSource.current
    val rootPath = source.rootPath
    var currentPath by remember(source) { mutableStateOf(rootPath) }
    var contents by remember(source, currentPath) { mutableStateOf<Result<List<FileEntry>>?>(null) }
    var expiry by remember { mutableStateOf(ShareExpiry.DAY_7) }
    var name by remember { mutableStateOf("") }
    var nameEdited by remember { mutableStateOf(false) }
    val suggestedName = if (currentPath != rootPath) currentPath.getFilenameFromPath()
    else if (source is com.ismartcoding.plain.rust.graphql.ContentSource.Remote) source.info.deviceName
    else getInternalStorageName()

    LaunchedEffect(source, currentPath) {
        val path = currentPath
        val result = loadFolderShareEntries {
            withIO { source.files.list(path, showHidden = false, sortBy = FileSortBy.NAME_ASC) }
        }
        contents = result
        if (result.isSuccess && !nameEdited) name = suggestedName
    }
    val entries = contents?.getOrNull()

    PModalBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.fillMaxWidth()) {
        PBottomSheetTopAppBar(
            title = stringResource(Res.string.share_folder),
        )
        Text(
            text = currentPath,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        VerticalSpace(8.dp)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
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
            items(entries.orEmpty().filter { it.isDir }, key = { it.path }) { dir ->
                DirBrowserRow(
                    name = dir.name,
                    onClick = { currentPath = dir.path },
                )
            }
        }
        VerticalSpace(8.dp)
        if (contents?.isFailure == true) {
            PAlert(stringResource(Res.string.load_failed), AlertType.ERROR, Modifier.padding(horizontal = 16.dp))
        } else {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (entries == null) stringResource(Res.string.loading)
                    else stringResource(Res.string.folder_card_items, entries.size) + " · " + entries.sumOf { if (it.isDir) 0L else it.size }.formatBytes(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        VerticalSpace(8.dp)
        ClipboardTextField(
            value = name,
            label = stringResource(Res.string.name),
            placeholder = stringResource(Res.string.share_name_placeholder),
            onValueChange = {
                name = it
                nameEdited = true
            },
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        VerticalSpace(8.dp)
        Text(
            text = stringResource(Res.string.share_expiry),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        VerticalSpace(8.dp)
        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ShareExpiry.entries.forEach { e ->
                PFilterChip(
                    selected = expiry == e,
                    onClick = { expiry = e },
                    label = { Text(stringResource(e.label)) },
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }
        VerticalSpace(8.dp)
        BottomActionButtons {
            PFilledButton(
                text = stringResource(Res.string.share_this_folder),
                enabled = contents?.isSuccess == true,
                onClick = { onConfirm(currentPath, name.ifBlank { suggestedName }, expiry) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

internal suspend fun loadFolderShareEntries(load: suspend () -> List<FileEntry>): Result<List<FileEntry>> =
    try {
        Result.success(load())
    } catch (canceled: CancellationException) {
        throw canceled
    } catch (failure: Exception) {
        Result.failure(failure)
    }
