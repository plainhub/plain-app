package com.ismartcoding.plain.ui.components.mediaviewer

import android.content.ClipData
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
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
import com.ismartcoding.plain.data.DImage
import com.ismartcoding.plain.data.DVideo
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.db.DTagRelation
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.features.media.ImageMediaStoreHelper
import com.ismartcoding.plain.features.media.VideoMediaStoreHelper
import com.ismartcoding.plain.helpers.QrCodeScanHelper
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.helpers.SvgHelper
import com.ismartcoding.plain.ui.base.ActionButtons
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.IconTextDeleteButton
import com.ismartcoding.plain.ui.base.IconTextRenameButton
import com.ismartcoding.plain.ui.base.IconTextScanQrCodeButton
import com.ismartcoding.plain.ui.base.IconTextShareButton
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.FileRenameDialog
import com.ismartcoding.plain.ui.components.ImageMetaRows
import com.ismartcoding.plain.ui.components.QrScanResultBottomSheet
import com.ismartcoding.plain.ui.components.TagSelector
import com.ismartcoding.plain.ui.components.VideoMetaRows
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.TagsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ViewMediaBottomSheet(
    m: PreviewItem,
    tagsVM: TagsViewModel? = null,
    tagsMap: Map<String, List<DTagRelation>>? = null,
    tagsState: List<DTag> = emptyList(),
    onDismiss: () -> Unit = {},
    onRenamedAsync: suspend () -> Unit = {},
    deleteAction: () -> Unit = {},
    onTagsChangedAsync: suspend () -> Unit = {},
) {
    var showRenameDialog by remember {
        mutableStateOf(false)
    }
    var showQrScanResult by remember { mutableStateOf(false) }
    var qrScanResult by remember { mutableStateOf("") }
    val context = LocalContext.current

    if (showRenameDialog) {
        FileRenameDialog(path = m.path, onDismiss = {
            showRenameDialog = false
        }, onDoneAsync = {
            m.path = m.path.substring(0, m.path.lastIndexOf("/") + 1) + it
            onRenamedAsync()
            onDismiss()
        })
    }

    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        if (m.data is DImage) {
            scope.launch(Dispatchers.IO) {
                try {
                    val bitmap = BitmapFactory.decodeFile(m.path)
                    if (bitmap != null) {
                        val result = QrCodeScanHelper.tryDecode(bitmap)
                        if (result != null) {
                            qrScanResult = result.text
                        }
                    }
                } catch (e: Exception) {
                }
            }
        }
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
            if (m.data is DImage || m.data is DVideo) {
                item {
                    ActionButtons {
                        if (qrScanResult.isNotEmpty()) {
                            IconTextScanQrCodeButton {
                                showQrScanResult = true
                            }
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

                        IconTextRenameButton {
                            showRenameDialog = true
                        }
                        IconTextDeleteButton {
                            DialogHelper.confirmToDelete {
                                deleteAction()
                                onDismiss()
                            }
                        }
                    }
                }
                item {
                    VerticalSpace(dp = 16.dp)
                    Subtitle(text = stringResource(id = R.string.tags))
                    TagSelector(
                        data = m.data,
                        tagsVM = tagsVM!!,
                        tagsMap = tagsMap!!,
                        tagsState = tagsState,
                        onChangedAsync = {
                            onTagsChangedAsync()
                        }
                    )
                    VerticalSpace(dp = 16.dp)
                }
            }

            item {
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
                    val mimeType = m.path.getMimeType()
                    PListItem(title = stringResource(id = R.string.type), value = mimeType)
                    val intrinsicSize = m.intrinsicSize
                    if (intrinsicSize.width > 0 && intrinsicSize.height > 0) {
                        PListItem(title = stringResource(id = R.string.dimensions), value = "${intrinsicSize.width}×${intrinsicSize.height}")
                    }
                    if (m.data is DImage) {
                        PListItem(title = stringResource(id = R.string.created_at), value = m.data.createdAt.formatDateTime())
                        PListItem(title = stringResource(id = R.string.updated_at), value = m.data.updatedAt.formatDateTime())
                        ImageMetaRows(path = m.path)
                    } else if (m.data is DVideo) {
                        PListItem(title = stringResource(id = R.string.created_at), value = m.data.createdAt.formatDateTime())
                        PListItem(title = stringResource(id = R.string.updated_at), value = m.data.updatedAt.formatDateTime())
                        VideoMetaRows(path = m.path)
                    } else if (m.path.isUrl()) {

                    } else if (mimeType.startsWith("image/")) {
                        ImageMetaRows(path = m.path)
                    } else if (mimeType.startsWith("video/")) {
                        VideoMetaRows(path = m.path)
                    }
                }
            }
            item {
                BottomSpace()
            }
        }
    }

    if (showQrScanResult) {
        QrScanResultBottomSheet(context, qrScanResult) {
            showQrScanResult = false
        }
    }
}


