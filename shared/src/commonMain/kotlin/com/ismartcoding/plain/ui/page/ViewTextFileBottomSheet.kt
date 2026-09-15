package com.ismartcoding.plain.ui.page
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.ui.theme.PlainTheme

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import org.jetbrains.compose.resources.stringResource
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.lib.extensions.formatBytes
import com.ismartcoding.plain.lib.extensions.getMimeType
import com.ismartcoding.plain.platform.deleteFileOrDir
import com.ismartcoding.plain.platform.formatDateTime
import com.ismartcoding.plain.platform.scanFiles
import com.ismartcoding.plain.platform.shareFiles
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.CopyIconButton
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.PSheetPrimaryAction
import com.ismartcoding.plain.ui.base.PSheetPrimaryActionsCard
import com.ismartcoding.plain.ui.base.PSwitch
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.TextFileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ViewTextFileBottomSheet(
    textFileVM: TextFileViewModel,
    path: String,
    m: DFile?,
    onDeleted: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val onDismiss = {
        textFileVM.showMoreActions.value = false
    }

    PModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
    ) {
        VerticalSpace(32.dp)
        PSheetPrimaryActionsCard {
            PSheetPrimaryAction(Res.drawable.share_2, stringResource(Res.string.share)) {
                shareFiles(listOf(path))
                onDismiss()
            }
            PSheetPrimaryAction(Res.drawable.arrow_up_to_line, stringResource(Res.string.jump_to_top)) {
                textFileVM.gotoTop()
                onDismiss()
            }
            PSheetPrimaryAction(Res.drawable.arrow_down_to_line, stringResource(Res.string.jump_to_bottom)) {
                textFileVM.gotoEnd()
                onDismiss()
            }
            PSheetPrimaryAction(
                Res.drawable.delete_forever,
                stringResource(Res.string.delete),
                container = MaterialTheme.colorScheme.errorContainer,
                tint = MaterialTheme.colorScheme.error,
            ) {
                DialogHelper.confirmToDelete {
                    scope.launch(Dispatchers.Default) {
                        val paths = mutableListOf(path)
                        paths.forEach {
                            deleteFileOrDir(it)
                        }
                        scanFiles(paths.toTypedArray())
                        onDismiss()
                        onDeleted()
                    }
                }
            }
        }
        VerticalSpace(dp = 24.dp)
        if (m != null) {
            PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                PListItem(title = m.path, action = {
                    CopyIconButton(text = m.path, clipLabel = stringResource(Res.string.file_path))
                })
            }
            VerticalSpace(dp = 16.dp)
            PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                PListItem(title = stringResource(Res.string.file_size), value = m.size.formatBytes())
                PListItem(title = stringResource(Res.string.type), value = m.path.getMimeType())
                m.createdAt?.let { createdAt ->
                    PListItem(title = stringResource(Res.string.created_at), value = createdAt.formatDateTime())
                }
                PListItem(title = stringResource(Res.string.updated_at), value = m.updatedAt.formatDateTime())
            }
            VerticalSpace(dp = 16.dp)
            PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                PListItem(title = stringResource(Res.string.wrap_content), action = {
                    PSwitch(
                        activated = textFileVM.controller.wrapContent.value,
                    ) {
                        textFileVM.toggleWrapContent()
                    }
                    HorizontalSpace(8.dp)
                })
            }
        }
        BottomSpace()
    }
}
