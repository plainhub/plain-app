@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ismartcoding.plain.ui.page.sharedfolder

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.platform.formatDateTime
import com.ismartcoding.plain.ui.base.NavigationCloseIcon
import com.ismartcoding.plain.ui.base.PCapsuleMoreClose
import com.ismartcoding.plain.ui.base.PSheetActionRow
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.platform.launchUrl
import com.ismartcoding.plain.platform.setClipboardText
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Top bar, styled after FilesPage: browse mode shows the current directory
 * with folder/file counts plus expiry as the subtitle; select mode swaps in
 * the close icon and the select-all text button.
 */
@Composable
internal fun SharedFolderTopBar(
    state: SharedFolderState,
    onClose: () -> Unit,
) {
    PTopAppBar(
        title = if (state.selectMode) {
            stringResource(Res.string.x_selected, state.selected.size)
        } else {
            state.crumbs.lastOrNull()?.name ?: state.rootInfo?.name ?: state.shareMsg?.name ?: ""
        },
        subtitle = if (state.selectMode) "" else browseSubtitle(state),
        navigationIcon = if (state.selectMode) {
            { NavigationCloseIcon { state.clearSelection() } }
        } else {
            null
        },
        actions = {
            if (state.selectMode) {
                PTextButton(
                    text = stringResource(
                        if (state.selected.containsAll(state.entries)) Res.string.unselect_all else Res.string.select_all,
                    ),
                    onClick = { state.toggleSelectAll() },
                )
            } else {
                PCapsuleMoreClose(
                    onClose = onClose,
                ) { dismiss ->
                    PSheetActionRow(Res.drawable.chrome, stringResource(Res.string.open_in_browser)) {
                        dismiss()
                        state.browserUrl()?.let { launchUrl(it) }
                    }
                    PSheetActionRow(Res.drawable.copy, stringResource(Res.string.copy)) {
                        dismiss()
                        state.browserUrl()?.let {
                            setClipboardText("", it)
                            DialogHelper.showSuccess(Res.string.copied)
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun browseSubtitle(state: SharedFolderState): String {
    val parts = mutableListOf<String>()
    val entries = state.entries
    val dirCount = entries.count { it.isDir }
    val fileCount = entries.count { !it.isDir }
    if (dirCount > 0) parts.add(pluralStringResource(Res.plurals.x_folders, dirCount, dirCount))
    if (fileCount > 0) parts.add(pluralStringResource(Res.plurals.x_files, fileCount, fileCount))
    val expiresAt = state.rootInfo?.expiresAtInstant
    if (expiresAt != null) {
        val dateText = expiresAt.formatDateTime()
        parts.add(
            if (expiresAt < TimeHelper.now()) dateText
            else stringResource(Res.string.share_expires_on, dateText),
        )
    }
    return parts.joinToString(" · ")
}
