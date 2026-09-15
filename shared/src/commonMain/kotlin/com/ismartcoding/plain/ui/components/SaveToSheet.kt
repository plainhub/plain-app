@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ismartcoding.plain.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.download
import com.ismartcoding.plain.i18n.folder
import com.ismartcoding.plain.i18n.folders
import org.jetbrains.compose.resources.DrawableResource
import com.ismartcoding.plain.i18n.download_to_downloads
import com.ismartcoding.plain.i18n.download_zip
import com.ismartcoding.plain.i18n.pick_directory
import com.ismartcoding.plain.platform.getDownloadsDirPath
import com.ismartcoding.plain.preferences.RecentSaveDirsPreference
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.PSheetActionCard
import com.ismartcoding.plain.ui.base.PSheetActionRow
import com.ismartcoding.plain.ui.base.VerticalSpace
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Reusable "save to this device" bottom sheet for any feature that writes
 * files locally. Offers the public Downloads dir, up to five recently used
 * custom folders (LRU via [RecentSaveDirsPreference], recorded here), the
 * folder picker, and — when [onZip] is set — a ZIP download. Pages only
 * supply the entry [title] and destination callbacks; the transfer itself
 * stays page-side.
 *
 * [downloadsAvailable] hides the Downloads row on platforms without one
 * (iOS); when it is false the callbacks still receive picked directories.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveToSheet(
    title: String,
    onDismiss: () -> Unit,
    onDownloads: () -> Unit,
    onDirectory: (dirPath: String) -> Unit,
    onZip: (() -> Unit)? = null,
    downloadsAvailable: Boolean = getDownloadsDirPath().isNotEmpty(),
) {
    val scope = rememberCoroutineScope()
    var recentDirs by remember { mutableStateOf<List<String>>(emptyList()) }
    var showFolderPick by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        recentDirs = RecentSaveDirsPreference.getValueAsync()
    }

    fun useDirectory(dir: String) {
        onDismiss()
        scope.launch { recentDirs = RecentSaveDirsPreference.recordAsync(dir) }
        onDirectory(dir)
    }

    PModalBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.fillMaxWidth()) {
        Column {
            PBottomSheetTopAppBar(
                title = title,
            )
            PSheetActionCard {
                if (downloadsAvailable) {
                    PSheetActionRow(Res.drawable.download, stringResource(Res.string.download_to_downloads)) {
                        onDismiss()
                        onDownloads()
                    }
                }
                recentDirs.forEach { dir ->
                    PListItem(
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { useDirectory(dir) },
                        title = dir.substringAfterLast('/'),
                        subtitle = dir,
                        icon = Res.drawable.folder,
                    )
                }
            }
            VerticalSpace(16.dp)
            PSheetActionCard {
                PSheetActionRow(Res.drawable.folders, stringResource(Res.string.pick_directory)) {
                    showFolderPick = true
                }
                if (onZip != null) {
                    PSheetActionRow(Res.drawable.download, stringResource(Res.string.download_zip)) {
                        onDismiss()
                        onZip()
                    }
                }
            }
            BottomSpace()
        }
    }

    if (showFolderPick) {
        FolderPickSheet(
            onDismiss = { showFolderPick = false },
            onConfirm = { dir ->
                showFolderPick = false
                useDirectory(dir)
            },
        )
    }
}
