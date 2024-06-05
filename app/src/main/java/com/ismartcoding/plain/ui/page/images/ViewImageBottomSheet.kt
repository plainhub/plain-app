package com.ismartcoding.plain.ui.page.images

import android.content.ClipData
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.OpenWith
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.ismartcoding.plain.features.media.ImageMediaStoreHelper
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.helpers.SvgHelper
import com.ismartcoding.plain.ui.base.ActionButtons
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PIconTextActionButton
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.components.ImageMetaRows
import com.ismartcoding.plain.ui.components.FileRenameDialog
import com.ismartcoding.plain.ui.components.TagSelector
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.ImagesViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ViewImageBottomSheet(
    viewModel: ImagesViewModel,
    tagsViewModel: TagsViewModel,
    tagsMap: Map<String, List<DTagRelation>>,
    tagsState: List<DTag>,
    dragSelectState: DragSelectState,
) {
    val m = viewModel.selectedItem.value ?: return
    val context = LocalContext.current
    val onDismiss = {
        viewModel.selectedItem.value = null
    }
    var viewSize by remember {
        mutableStateOf(m.getRotatedSize())
    }

    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            if (m.path.endsWith(".svg", true)) {
                viewSize = SvgHelper.getSize(m.path)
            }
        }
    }

    if (viewModel.showRenameDialog.value) {
        FileRenameDialog(path = m.path, onDismiss = {
            viewModel.showRenameDialog.value = false
        }, onDone = {
            scope.launch(Dispatchers.IO) { viewModel.loadAsync(context, tagsViewModel) }
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
                ActionButtons {
                    if (!viewModel.showSearchBar.value) {
                        PIconTextActionButton(
                            icon = Icons.Outlined.Checklist,
                            text = LocaleHelper.getString(R.string.select),
                            click = {
                                dragSelectState.enterSelectMode()
                                dragSelectState.select(m.id)
                                onDismiss()
                            }
                        )
                    }
                    PIconTextActionButton(
                        icon = Icons.Outlined.Share,
                        text = LocaleHelper.getString(R.string.share),
                        click = {
                            ShareHelper.shareUris(context, listOf(ImageMediaStoreHelper.getItemUri(m.id)))
                            onDismiss()
                        }
                    )
                    if (!m.path.isUrl()) {
                        PIconTextActionButton(
                            icon = Icons.Outlined.OpenWith,
                            text = LocaleHelper.getString(R.string.open_with),
                            click = {
                                ShareHelper.openPathWith(context, m.path)
                            }
                        )
                    }
                    PIconTextActionButton(
                        icon = Icons.Outlined.Edit,
                        text = LocaleHelper.getString(R.string.rename),
                        click = {
                            viewModel.showRenameDialog.value = true
                        }
                    )
                    PIconTextActionButton(
                        icon = Icons.Outlined.DeleteForever,
                        text = LocaleHelper.getString(R.string.delete),
                        click = {
                            DialogHelper.confirmToDelete {
                                viewModel.delete(context, tagsViewModel, setOf(m.id))
                                onDismiss()
                            }
                        }
                    )
                }
            }
            if (!viewModel.trash.value) {
                item {
                    VerticalSpace(dp = 16.dp)
                    Subtitle(text = stringResource(id = R.string.tags))
                    TagSelector(
                        data = m,
                        tagsViewModel = tagsViewModel,
                        tagsMap = tagsMap,
                        tagsState = tagsState,
                        onChanged = {
                            scope.launch(Dispatchers.IO) {
                                viewModel.refreshTabsAsync(context, tagsViewModel)
                            }
                        }
                    )
                }
            }
            item {
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
            }
            item {
                VerticalSpace(dp = 16.dp)
                PCard {
                    PListItem(title = stringResource(id = R.string.file_size), value = m.size.formatBytes())
                    PListItem(title = stringResource(id = R.string.type), value = m.path.getMimeType())
                    PListItem(title = stringResource(id = R.string.dimensions), value = "${viewSize.width}×${viewSize.height}")
                    PListItem(title = stringResource(id = R.string.created_at), value = m.createdAt.formatDateTime())
                    PListItem(title = stringResource(id = R.string.updated_at), value = m.updatedAt.formatDateTime())
                    ImageMetaRows(path = m.path)
                }
            }
            item {
                BottomSpace()
            }
        }
    }
}


