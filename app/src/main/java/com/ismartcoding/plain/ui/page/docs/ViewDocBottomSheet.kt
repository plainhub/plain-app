package com.ismartcoding.plain.ui.page.docs

import android.content.ClipData
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
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
import com.ismartcoding.plain.ui.base.ActionButtons
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.IconTextDeleteButton
import com.ismartcoding.plain.ui.base.IconTextRenameButton
import com.ismartcoding.plain.ui.base.IconTextSelectButton
import com.ismartcoding.plain.ui.base.IconTextShareButton
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
    docsVM: DocsViewModel,
) {
    val context = LocalContext.current
    val m = docsVM.selectedItem.value ?: return
    val onDismiss = {
        docsVM.selectedItem.value = null
    }



    if (docsVM.showRenameDialog.value) {
        FileRenameDialog(path = m.path, onDismiss = {
            docsVM.showRenameDialog.value = false
        }, onDoneAsync = {
            m.path = it
            m.name = it.getFilenameFromPath()
        })
    }

    PModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
    ) {
        LazyColumn {
            item {
                VerticalSpace(32.dp)
            }
            item {
                ActionButtons {
                    if (!docsVM.showSearchBar.value) {
                        IconTextSelectButton {
                            docsVM.enterSelectMode()
                            docsVM.select(m.id)
                            onDismiss()
                        }
                    }
                    IconTextShareButton {
                        ShareHelper.sharePaths(context, setOf(m.path))
                        onDismiss()
                    }
                    IconTextRenameButton {
                        docsVM.showRenameDialog.value = true
                    }
                    IconTextDeleteButton {
                        DialogHelper.confirmToDelete {
                            docsVM.delete(setOf(m.id))
                            onDismiss()
                        }
                    }
                }
                VerticalSpace(dp = 24.dp)
                PCard {
                    PListItem(title = m.path, action = {
                        PIconButton(icon = R.drawable.copy, contentDescription = stringResource(id = R.string.copy_path), click = {
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
            }
            item {
                BottomSpace()
            }
        }
    }
}


