@file:OptIn(ExperimentalMaterial3Api::class)

package com.ismartcoding.plain.ui.shares

import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.models.ManagedStorageRoot
import com.ismartcoding.plain.ui.navigation.LocalContentSource
import com.ismartcoding.plain.ui.state.launchSafe
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.ui.i18n.LocaleHelper
import com.ismartcoding.plain.lib.logcat.LogCat

import com.ismartcoding.plain.features.files.extension

import com.ismartcoding.plain.models.File as FileEntry

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.features.files.FileSortBy
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.extensions.formatBytes
import com.ismartcoding.plain.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.ui.base.BottomActionButtons
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.components.DirBrowserRow
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.material3.ExperimentalMaterial3Api

@Composable
fun ShareItemsPickerSheet(
    initialSelected: Map<String, Boolean>,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Boolean>) -> Unit,
    source: ContentSource = LocalContentSource.current,
) {
    var rootPath by remember(source) { mutableStateOf(source.rootPath) }
    var currentPath by remember(source) { mutableStateOf<String?>(rootPath) }
    var roots by remember(source) { mutableStateOf<List<ManagedStorageRoot>>(emptyList()) }
    val appDataTitle = stringResource(Res.string.app_data)
    var entries by remember(source) { mutableStateOf<List<FileEntry>>(emptyList()) }
    val selected = remember(source) { mutableStateMapOf<String, Boolean>().apply { putAll(initialSelected) } }

    LaunchedEffect(currentPath, source) {
        launchSafe(onError = {
            LogCat.e("Share picker directory", it)
            DialogHelper.showErrorMessage(LocaleHelper.getString(Res.string.load_failed))
        }) {
            val path = currentPath
            if (path == null && source is ContentSource.Remote) {
                roots = withIO { source.client.roots() }
            } else if (path != null) {
                entries = withIO { source.files.list(path, showHidden = false, sortBy = FileSortBy.NAME_ASC) }
            }
        }.join()
    }

    fun toggle(entry: FileEntry) {
        if (selected.containsKey(entry.path)) selected.remove(entry.path) else selected[entry.path] = entry.isDir
    }

    PModalBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.fillMaxWidth()) {
        PBottomSheetTopAppBar(
            title = stringResource(Res.string.add_items),
        )
        Text(
            text = currentPath ?: stringResource(Res.string.folders),
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
            val path = currentPath
            if (path != null && (path != rootPath || source is ContentSource.Remote)) {
                item {
                    DirBrowserRow(
                        name = "..",
                        subtitle = stringResource(Res.string.back),
                        onClick = {
                            currentPath = if (path == rootPath) null else path.substringBeforeLast('/', missingDelimiterValue = rootPath).ifEmpty { rootPath }
                        },
                    )
                }
            }
            if (path == null) {
                items((roots + ManagedStorageRoot(appDataTitle, source.appPath)).distinctBy { it.path }, key = { it.path }) { root ->
                    DirBrowserRow(
                        name = root.name.ifEmpty { root.path },
                        subtitle = root.path,
                        onClick = { rootPath = root.path; currentPath = root.path },
                    )
                }
            }
            items(if (path == null) emptyList() else entries, key = { it.path }) { entry ->
                DirBrowserRow(
                    name = entry.name,
                    subtitle = if (entry.isDir) "" else entry.size.formatBytes(),
                    isDir = entry.isDir,
                    selected = selected.containsKey(entry.path),
                    showCheckbox = true,
                    onClick = {
                        if (entry.isDir) currentPath = entry.path else toggle(entry)
                    },
                    onCheckedChange = { toggle(entry) },
                )
            }
        }
        BottomActionButtons {
            PFilledButton(
                text = stringResource(Res.string.done) + " (${selected.size})",
                onClick = { onConfirm(selected.toMap()) },
                enabled = selected.isNotEmpty(),
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
            )
        }
    }
}
