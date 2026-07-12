package com.ismartcoding.plain.ui.components.mediaviewer

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.data.DImage
import com.ismartcoding.plain.data.DVideo
import com.ismartcoding.plain.platform.shareFile
import com.ismartcoding.plain.ui.base.ActionButtons
import com.ismartcoding.plain.ui.base.CopyIconButton
import com.ismartcoding.plain.ui.base.IconTextDeleteButton
import com.ismartcoding.plain.ui.base.IconTextRenameButton
import com.ismartcoding.plain.ui.base.IconTextScanQrCodeButton
import com.ismartcoding.plain.ui.base.IconTextShareButton
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.helpers.DialogHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ViewMediaActionButtons(
    m: PreviewItem,
    qrScanResult: String,
    onShowQrScanResult: () -> Unit,
    onShowRenameDialog: () -> Unit,
    deleteAction: () -> Unit,
    onDismiss: () -> Unit,
) {
    ActionButtons {
        if (qrScanResult.isNotEmpty()) {
            IconTextScanQrCodeButton { onShowQrScanResult() }
        }
        if (m.data is DImage || m.data is DVideo) {
            IconTextShareButton {
                shareFile(m.path)
                onDismiss()
            }
        }
        IconTextRenameButton { onShowRenameDialog() }
        IconTextDeleteButton {
            DialogHelper.confirmToDelete {
                deleteAction()
                onDismiss()
            }
        }
    }
}

@Composable
internal fun ViewMediaPathCard(m: PreviewItem) {
    PCard {
        PListItem(title = m.path, action = {
            CopyIconButton(text = m.path, clipLabel = stringResource(Res.string.file_path))
        })
    }
}
