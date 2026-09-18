@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ismartcoding.plain.ui.page.sharedfolder

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.launchUrl
import com.ismartcoding.plain.platform.setClipboardText
import com.ismartcoding.plain.ui.base.NavigationCloseIcon
import com.ismartcoding.plain.ui.base.PCapsuleMoreClose
import com.ismartcoding.plain.ui.base.PSheetActionRow
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.helpers.DialogHelper
import org.jetbrains.compose.resources.stringResource

/** Top bar: select-mode title/actions, otherwise the capsule link menu. */
@Composable
internal fun SharedFolderTopBar(
    state: SharedFolderState,
    onClose: () -> Unit,
) {
    PTopAppBar(
        title = if (state.selectMode) {
            stringResource(Res.string.x_selected, state.selected.size)
        } else {
            state.rootInfo?.name ?: state.shareMsg?.name ?: ""
        },
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
