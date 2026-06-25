package com.ismartcoding.plain.ui.page.videos

import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.R
import android.content.ClipData
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.lib.extensions.isUrl
import com.ismartcoding.plain.clipboardManager
import com.ismartcoding.plain.data.DVideo
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.db.DTagRelation
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.features.media.VideoMediaStoreHelper
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.ui.base.ActionButtons
import com.ismartcoding.plain.ui.base.CopyIconButton
import com.ismartcoding.plain.ui.base.IconTextAddToHomeButton
import com.ismartcoding.plain.ui.components.AddToHomeDialog
import com.ismartcoding.plain.ui.base.IconTextDeleteButton
import com.ismartcoding.plain.ui.base.IconTextOpenWithButton
import com.ismartcoding.plain.ui.base.IconTextRenameButton
import com.ismartcoding.plain.ui.base.IconTextRestoreButton
import com.ismartcoding.plain.ui.base.IconTextSelectButton
import com.ismartcoding.plain.ui.base.IconTextShareButton
import com.ismartcoding.plain.ui.base.IconTextTrashButton
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.VideosViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VideoActionButtons(
    m: DVideo,
    videosVM: VideosViewModel,
    tagsVM: TagsViewModel,
    dragSelectState: DragSelectState,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var showAddToHomeDialog by remember { mutableStateOf(false) }
    ActionButtons {
        if (!videosVM.showSearchBar.value) {
            IconTextSelectButton {
                dragSelectState.enterSelectMode()
                dragSelectState.select(m.id)
                onDismiss()
            }
        }
        IconTextShareButton {
            ShareHelper.shareUris(context, listOf(VideoMediaStoreHelper.getItemUri(m.id)))
            onDismiss()
        }
        if (!m.path.isUrl()) {
            IconTextOpenWithButton {
                ShareHelper.openPathWith(context, m.path)
            }
        }
        if (!m.path.isUrl() && !videosVM.trash.value) {
            IconTextAddToHomeButton {
                showAddToHomeDialog = true
            }
        }
        IconTextRenameButton {
            videosVM.showRenameDialog.value = true
        }
        if (AppFeatureType.MEDIA_TRASH.has()) {
            if (videosVM.trash.value) {
                IconTextRestoreButton {
                    videosVM.restore(context, tagsVM, setOf(m.id))
                    onDismiss()
                }
                IconTextDeleteButton {
                    DialogHelper.confirmToDelete {
                        videosVM.delete(context, tagsVM, setOf(m.id))
                        onDismiss()
                    }
                }
            } else {
                IconTextTrashButton {
                    videosVM.trash(context, tagsVM, setOf(m.id))
                    onDismiss()
                }
            }
        } else {
            IconTextDeleteButton {
                DialogHelper.confirmToDelete {
                    videosVM.delete(context, tagsVM, setOf(m.id))
                    onDismiss()
                }
            }
        }
    }
    if (showAddToHomeDialog) {
        AddToHomeDialog(path = m.path, iconRes = R.mipmap.ic_launcher, onDismiss = {
            showAddToHomeDialog = false
            onDismiss()
        })
    }
}

@Composable
internal fun VideoPathCard(m: DVideo) {
    PCard {
        PListItem(title = m.path, action = {
            CopyIconButton(text = m.path, clipLabel = stringResource(Res.string.file_path))
        })
    }
}
