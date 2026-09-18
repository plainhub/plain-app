package com.ismartcoding.plain.ui.page.images
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.ui.theme.PlainTheme

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.has
import com.ismartcoding.plain.lib.extensions.getFilenameWithoutExtensionFromPath
import com.ismartcoding.plain.lib.extensions.isUrl
import com.ismartcoding.plain.platform.addMediaShortcut
import com.ismartcoding.plain.platform.getMediaItemUriString
import com.ismartcoding.plain.platform.openFileExternal
import com.ismartcoding.plain.platform.shareFiles
import com.ismartcoding.plain.ui.base.CopyIconButton
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PSheetActionRow
import com.ismartcoding.plain.ui.base.PSheetPrimaryAction
import com.ismartcoding.plain.ui.base.PSheetPrimaryActionsCard
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.components.AddToHomeDialog
import com.ismartcoding.plain.ui.components.AddToHomeHelpAction
import com.ismartcoding.plain.ui.helpers.confirmActionAsync
import com.ismartcoding.plain.ui.models.ImagesViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.enums.DataType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ViewImageActionButtons(
    imagesVM: ImagesViewModel,
    tagsVM: TagsViewModel,
    m: com.ismartcoding.plain.data.DImage,
    dragSelectState: DragSelectState,
    qrScanResult: String,
    onShowQrScanResult: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var showAddToHomeDialog by remember { mutableStateOf(false) }
    PSheetPrimaryActionsCard {
        if (!imagesVM.showSearchBar.value) {
            PSheetPrimaryAction(Res.drawable.list_checks, stringResource(Res.string.select)) {
                dragSelectState.enterSelectMode()
                dragSelectState.select(m.id)
                onDismiss()
            }
        }
        PSheetPrimaryAction(Res.drawable.share_2, stringResource(Res.string.share)) {
            shareFiles(listOf(getMediaItemUriString(DataType.IMAGE, m.id)))
            onDismiss()
        }
        // At most 4 disc actions per card: open-with and rename drop to the
        // secondary rows whenever the trash view or delete crowds the row.
        if (!m.path.isUrl() && !imagesVM.trash.value) {
            PSheetPrimaryAction(Res.drawable.square_arrow_out_up_right, stringResource(Res.string.open_with)) {
                openFileExternal(m.path)
            }
        }
        if (AppFeatureType.MEDIA_TRASH.has() && !imagesVM.trash.value) {
            PSheetPrimaryAction(Res.drawable.pen, stringResource(Res.string.rename)) {
                imagesVM.showRenameDialog.value = true
            }
        }
        if (AppFeatureType.MEDIA_TRASH.has() && imagesVM.trash.value) {
            PSheetPrimaryAction(Res.drawable.archive_restore, stringResource(Res.string.restore)) {
                imagesVM.restore(tagsVM, setOf(m.id))
                onDismiss()
            }
        }
        if (!AppFeatureType.MEDIA_TRASH.has() || imagesVM.trash.value) {
            PSheetPrimaryAction(
                Res.drawable.delete_forever,
                stringResource(Res.string.delete),
                container = MaterialTheme.colorScheme.errorContainer,
                tint = MaterialTheme.colorScheme.error,
            ) {
                scope.launch {
                    confirmActionAsync(
                        Res.string.delete,
                        Res.string.confirm_to_delete,
                        callback = {
                            imagesVM.delete(tagsVM, setOf(m.id))
                            onDismiss()
                        },
                        danger = true
                    )
                }
            }
        }
    }
    val hasSecondary = qrScanResult.isNotEmpty() ||
        (!m.path.isUrl() && !imagesVM.trash.value) ||
        (AppFeatureType.MEDIA_TRASH.has() && !imagesVM.trash.value) ||
        imagesVM.trash.value || !AppFeatureType.MEDIA_TRASH.has()
    if (hasSecondary) {
        VerticalSpace(12.dp)
        PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
            Column {
                if (qrScanResult.isNotEmpty()) {
                    PSheetActionRow(Res.drawable.scan_qr_code, stringResource(Res.string.scan_qrcode)) {
                        onShowQrScanResult()
                    }
                }
                if (!m.path.isUrl() && !imagesVM.trash.value) {
                    PSheetActionRow(Res.drawable.smartphone, stringResource(Res.string.add_to_home), trailing = { AddToHomeHelpAction() }) {
                        showAddToHomeDialog = true
                    }
                }
                if (AppFeatureType.MEDIA_TRASH.has() && !imagesVM.trash.value) {
                    PSheetActionRow(Res.drawable.trash_2, stringResource(Res.string.trash)) {
                        imagesVM.trash(tagsVM, setOf(m.id))
                        onDismiss()
                    }
                }
                if (!m.path.isUrl() && imagesVM.trash.value) {
                    PSheetActionRow(Res.drawable.square_arrow_out_up_right, stringResource(Res.string.open_with)) {
                        openFileExternal(m.path)
                    }
                }
                if (imagesVM.trash.value || !AppFeatureType.MEDIA_TRASH.has()) {
                    PSheetActionRow(Res.drawable.pen, stringResource(Res.string.rename)) {
                        imagesVM.showRenameDialog.value = true
                    }
                }
            }
        }
    }
    if (showAddToHomeDialog) {
        AddToHomeDialog(
            defaultLabel = remember(m.path) { m.path.getFilenameWithoutExtensionFromPath() },
            onAddToHome = { label -> addMediaShortcut(m.path, label) },
            onDismiss = {
                showAddToHomeDialog = false
                onDismiss()
            })
    }
}

@Composable
internal fun ViewImagePathCard(
    m: com.ismartcoding.plain.data.DImage,
) {
    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
        PListItem(title = m.path, action = {
            CopyIconButton(text = m.path, clipLabel = stringResource(Res.string.file_path))
        })
    }
}
