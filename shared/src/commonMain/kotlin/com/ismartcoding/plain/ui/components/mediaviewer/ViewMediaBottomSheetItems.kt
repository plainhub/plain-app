package com.ismartcoding.plain.ui.components.mediaviewer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.theme.PlainTheme

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.data.DImage
import com.ismartcoding.plain.data.DVideo
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.platform.shareFile
import com.ismartcoding.plain.ui.base.CopyIconButton
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PSheetActionRow
import com.ismartcoding.plain.ui.base.PSheetPrimaryAction
import com.ismartcoding.plain.ui.base.PSheetPrimaryDeleteAction
import com.ismartcoding.plain.ui.base.PSheetPrimaryActionsCard
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.helpers.confirmActionAsync

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ViewMediaActionButtons(
    m: PreviewItem,
    qrScanResult: String,
    onShowQrScanResult: () -> Unit,
    onShowRenameDialog: () -> Unit,
    deleteAction: () -> Unit,
    onDismiss: () -> Unit,
    onCast: (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val isMediaFile = m.data is DImage || m.data is DVideo
    // At most 4 disc actions per card: rename drops to a secondary row when
    // qr-result and cast discs would crowd delete off the row.
    val renameInPrimary = !(isMediaFile && qrScanResult.isNotEmpty() && onCast != null)
    PSheetPrimaryActionsCard {
        PSheetPrimaryAction(Res.drawable.share_2, stringResource(Res.string.share)) {
            shareFile(m.path)
            onDismiss()
        }
        if (qrScanResult.isNotEmpty()) {
            PSheetPrimaryAction(Res.drawable.scan_qr_code, stringResource(Res.string.scan_qrcode)) {
                onShowQrScanResult()
            }
        }
        if (onCast != null) {
            PSheetPrimaryAction(Res.drawable.cast, stringResource(Res.string.cast)) { onCast() }
        }
        if (isMediaFile) {
            if (renameInPrimary) {
                PSheetPrimaryAction(Res.drawable.pen, stringResource(Res.string.rename)) {
                    onShowRenameDialog()
                }
            }
            PSheetPrimaryDeleteAction(stringResource(Res.string.delete)) {
                scope.launch {
                    confirmActionAsync(
                        Res.string.delete,
                        Res.string.confirm_to_delete,
                        callback = {
                            deleteAction()
                            onDismiss()
                        },
                        danger = true
                    )
                }
            }
        }
    }
    if (!renameInPrimary) {
        VerticalSpace(12.dp)
        PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
            PSheetActionRow(Res.drawable.pen, stringResource(Res.string.rename)) {
                onShowRenameDialog()
            }
        }
    }
}

@Composable
internal fun ViewMediaPathCard(m: PreviewItem) {
    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
        PListItem(title = m.path, action = {
            CopyIconButton(text = m.path, clipLabel = stringResource(Res.string.file_path))
        })
    }
}
