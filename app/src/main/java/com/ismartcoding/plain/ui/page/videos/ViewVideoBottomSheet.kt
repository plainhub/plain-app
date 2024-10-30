package com.ismartcoding.plain.ui.page.videos

import android.content.ClipData
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.extensions.formatBytes
import com.ismartcoding.lib.extensions.getMimeType
import com.ismartcoding.lib.extensions.isUrl
import com.ismartcoding.plain.R
import com.ismartcoding.plain.clipboardManager
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.db.DTagRelation
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.features.media.VideoMediaStoreHelper
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.ui.base.ActionButtons
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.IconTextDeleteButton
import com.ismartcoding.plain.ui.base.IconTextOpenWithButton
import com.ismartcoding.plain.ui.base.IconTextRenameButton
import com.ismartcoding.plain.ui.base.IconTextSelectButton
import com.ismartcoding.plain.ui.base.IconTextShareButton
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.components.FileRenameDialog
import com.ismartcoding.plain.ui.components.TagSelector
import com.ismartcoding.plain.ui.components.VideoMetaRows
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.VideosViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ViewVideoBottomSheet(
    videosVM: VideosViewModel,
    tagsVM: TagsViewModel,
    tagsMap: Map<String, List<DTagRelation>>,
    tagsState: List<DTag>,
    dragSelectState: DragSelectState,
) {
    val m = videosVM.selectedItem.value ?: return
    val context = LocalContext.current
    val onDismiss = {
        videosVM.selectedItem.value = null
    }
    val viewSize by remember {
        mutableStateOf(m.getRotatedSize())
    }

    val scope = rememberCoroutineScope()

    if (videosVM.showRenameDialog.value) {
        FileRenameDialog(path = m.path, onDismiss = {
            videosVM.showRenameDialog.value = false
        }, onDone = {
            scope.launch(Dispatchers.IO) { videosVM.loadAsync(context, tagsVM) }
            onDismiss()
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
                    IconTextRenameButton {
                        videosVM.showRenameDialog.value = true
                    }
                    IconTextDeleteButton {
                        DialogHelper.confirmToDelete {
                            videosVM.delete(context, tagsVM, setOf(m.id))
                            onDismiss()
                        }
                    }
                }
            }
            if (!videosVM.trash.value) {
                item {
                    VerticalSpace(dp = 16.dp)
                    Subtitle(text = stringResource(id = R.string.tags))
                    TagSelector(
                        data = m,
                        tagsVM = tagsVM,
                        tagsMap = tagsMap,
                        tagsState = tagsState,
                        onChanged = {
                        }
                    )
                }
            }
            item {
                VerticalSpace(dp = 16.dp)
                PCard {
                    PListItem(title = m.path, action = {
                        PIconButton(icon = R.drawable.copy, contentDescription = stringResource(id = R.string.copy_path), click = {
                            val clip = ClipData.newPlainText(LocaleHelper.getString(R.string.file_path), m.path)
                            clipboardManager.setPrimaryClip(clip)
                            DialogHelper.showTextCopiedMessage(m.path)
                        })
                    })
                }
            }
            item {
                VerticalSpace(dp = 16.dp)
                PCard {
                    PListItem(title = stringResource(id = R.string.file_size), value = m.size.formatBytes())
                    PListItem(title = stringResource(id = R.string.type), value = m.path.getMimeType())
                    PListItem(title = stringResource(id = R.string.dimensions), value = "${viewSize.width}×${viewSize.height}")
                    PListItem(title = stringResource(id = R.string.created_at), value = m.createdAt.formatDateTime())
                    PListItem(title = stringResource(id = R.string.updated_at), value = m.updatedAt.formatDateTime())
                    VideoMetaRows(path = m.path)
                }
            }
            item {
                BottomSpace()
            }
        }
    }
}


