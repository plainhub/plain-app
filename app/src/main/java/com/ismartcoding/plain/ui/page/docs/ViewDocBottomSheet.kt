package com.ismartcoding.plain.ui.page.docs

import android.content.ClipData
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.extensions.formatBytes
import com.ismartcoding.lib.extensions.getFilenameFromPath
import com.ismartcoding.lib.extensions.getMimeType
import com.ismartcoding.plain.R
import com.ismartcoding.plain.clipboardManager
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.GroupButton
import com.ismartcoding.plain.ui.base.GroupButtons
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.FileRenameDialog
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.DocsViewModel
import com.ismartcoding.plain.ui.models.enterSelectMode
import com.ismartcoding.plain.ui.models.select

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ViewDocBottomSheet(
    viewModel: DocsViewModel,
) {
    val context = LocalContext.current
    val m = viewModel.selectedItem.value ?: return
    val onDismiss = {
        viewModel.selectedItem.value = null
    }
    val scope = rememberCoroutineScope()
    val groupButtons = remember { mutableStateListOf<GroupButton>() }
    LaunchedEffect(Unit) {
        if (!viewModel.showSearchBar.value) {
            groupButtons.add(
                GroupButton(
                    icon = Icons.Outlined.Checklist,
                    text = LocaleHelper.getString(R.string.select),
                    onClick = {
                        viewModel.enterSelectMode()
                        viewModel.select(m.id)
                        onDismiss()
                    }
                )
            )
        }
        groupButtons.addAll(
            listOf(
                GroupButton(
                    icon = Icons.Outlined.Share,
                    text = LocaleHelper.getString(R.string.share),
                    onClick = {
                        ShareHelper.sharePaths(context, setOf(m.path))
                        onDismiss()
                    }
                ),
                GroupButton(
                    icon = Icons.Outlined.Edit,
                    text = LocaleHelper.getString(R.string.rename),
                    onClick = {
                        viewModel.showRenameDialog.value = true
                    }
                ),
                GroupButton(
                    icon = Icons.Outlined.DeleteForever,
                    text = LocaleHelper.getString(R.string.delete),
                    onClick = {
                        DialogHelper.confirmToDelete {
                            viewModel.delete(setOf(m.id))
                            onDismiss()
                        }
                    }
                )
            ))
    }

    if (viewModel.showRenameDialog.value) {
        FileRenameDialog(path = m.path, onDismiss = {
            viewModel.showRenameDialog.value = false
        }, onDone = {
            m.path = it
            m.name = it.getFilenameFromPath()
        })
    }

    PModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
    ) {
        GroupButtons(
            buttons = groupButtons
        )
        VerticalSpace(dp = 24.dp)
        PCard {
            PListItem(title = m.path, action = {
                PIconButton(icon = Icons.Outlined.ContentCopy, contentDescription = stringResource(id = R.string.copy_path), onClick = {
                    val clip = ClipData.newPlainText(LocaleHelper.getString(R.string.file_path), m.path)
                    clipboardManager.setPrimaryClip(clip)
                    DialogHelper.showTextCopiedMessage(m.path)
                })
            })
        }
        VerticalSpace(dp = 16.dp)
        PCard {
            PListItem(title = stringResource(id = R.string.file_size), value = m.size.formatBytes())
            PListItem(title = stringResource(id = R.string.type), value = m.path.getMimeType())
            if (m.createdAt != null) {
                PListItem(title = stringResource(id = R.string.created_at), value = m.createdAt.formatDateTime())
            }
            PListItem(title = stringResource(id = R.string.updated_at), value = m.updatedAt.formatDateTime())
        }
        BottomSpace()
    }
}


