package com.ismartcoding.plain.ui.components.mediaviewer

import android.content.ClipData
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ismartcoding.plain.R
import com.ismartcoding.plain.clipboardManager
import com.ismartcoding.plain.data.DImage
import com.ismartcoding.plain.data.DVideo
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.features.media.ImageMediaStoreHelper
import com.ismartcoding.plain.features.media.VideoMediaStoreHelper
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.ui.base.ActionButtons
import com.ismartcoding.plain.ui.base.CopyIconButton
import com.ismartcoding.plain.ui.base.IconTextDeleteButton
import com.ismartcoding.plain.ui.base.IconTextRenameButton
import com.ismartcoding.plain.ui.base.IconTextScanQrCodeButton
import com.ismartcoding.plain.ui.base.IconTextShareButton
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PIconButton
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
    val context = LocalContext.current

    ActionButtons {
        if (qrScanResult.isNotEmpty()) {
            IconTextScanQrCodeButton { onShowQrScanResult() }
        }
        if (m.data is DImage || m.data is DVideo) {
            IconTextShareButton {
                if (m.data is DImage) {
                    ShareHelper.shareUris(context, listOf(ImageMediaStoreHelper.getItemUri(m.id)))
                } else {
                    ShareHelper.shareUris(context, listOf(VideoMediaStoreHelper.getItemUri(m.id)))
                }
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
            CopyIconButton(text = m.path, clipLabel = stringResource(R.string.file_path))
        })
    }
}
