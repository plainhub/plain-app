package com.ismartcoding.plain.ui.images


import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import com.ismartcoding.plain.ui.extensions.getRotatedSize
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.models.Tag
import com.ismartcoding.plain.models.TagRelation
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.extensions.formatBytes
import com.ismartcoding.plain.lib.extensions.getMimeType
import com.ismartcoding.plain.ui.platform.media.ImageMetaRows
import com.ismartcoding.plain.platform.system.formatDateTime
import com.ismartcoding.plain.ui.platform.media.formatImageMetaTexts
import com.ismartcoding.plain.ui.platform.media.getSvgSize
import com.ismartcoding.plain.ui.platform.media.tryDecodeQrCode
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.PSectionTitle
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.components.FileRenameDialog
import com.ismartcoding.plain.ui.components.QrScanResultBottomSheet
import com.ismartcoding.plain.ui.components.TagSelector
import com.ismartcoding.plain.ui.images.ImagesViewModel
import com.ismartcoding.plain.ui.tags.TagsViewModel
import com.ismartcoding.plain.ui.theme.PlainTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.ismartcoding.plain.ui.state.launchSafe
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ViewImageBottomSheet(
    imagesVM: ImagesViewModel,
    tagsVM: TagsViewModel,
    tagsMap: Map<String, List<TagRelation>>,
    tagsState: List<Tag>,
    dragSelectState: DragSelectState,
) {
    val m = imagesVM.selectedItem.value ?: return
    val onDismiss = { imagesVM.selectedItem.value = null }
    var viewSize by remember { mutableStateOf(m.getRotatedSize()) }
    var showQrScanResult by remember { mutableStateOf(false) }
    var qrScanResult by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        scope.launchSafe {
            kotlinx.coroutines.withContext(Dispatchers.Default) {
                if (m.path.endsWith(".svg", true)) {
                    viewSize = if (imagesVM.source is com.ismartcoding.plain.rust.graphql.ContentSource.Local) getSvgSize(m.path)
                        else imagesVM.source.media.imageInfo(m.path)?.let { androidx.compose.ui.unit.IntSize(it.width, it.height) } ?: viewSize
                }
                qrScanResult = tryDecodeQrCode(m.path, imagesVM.source).orEmpty()
            }
        }
    }

    if (imagesVM.showRenameDialog.value) {
        FileRenameDialog(path = m.path, onDismiss = {
            imagesVM.showRenameDialog.value = false
        }, onRename = { p, name -> imagesVM.source.files.rename(p, name) }, onRenamed = {
            imagesVM.loadAsync(tagsVM)
            onDismiss()
        })
    }

    PModalBottomSheet(onDismissRequest = { onDismiss() }) {
        LazyColumn {
            item { VerticalSpace(16.dp) }
            item {
                ViewImageActionButtons(
                    imagesVM = imagesVM, tagsVM = tagsVM, m = m,
                    dragSelectState = dragSelectState, qrScanResult = qrScanResult,
                    onShowQrScanResult = { showQrScanResult = true }, onDismiss = onDismiss,
                )
            }
            if (!imagesVM.trash.value) {
                item {
                    VerticalSpace(dp = 16.dp)
                    PSectionTitle(title = stringResource(Res.string.tags))
                    TagSelector(data = m, tagsVM = tagsVM, tagsMap = tagsMap, tagsState = tagsState,
                        onChangedAsync = { imagesVM.loadAsync(tagsVM) })
                }
            }
            item {
                VerticalSpace(dp = 16.dp)
                ViewImagePathCard(m = m)
            }
            item {
                VerticalSpace(dp = 16.dp)
                PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                    PListItem(title = stringResource(Res.string.file_size), value = m.size.formatBytes())
                    PListItem(title = stringResource(Res.string.type), value = m.path.getMimeType())
                    PListItem(title = stringResource(Res.string.dimensions), value = "${viewSize.width}\u00d7${viewSize.height}")
                    PListItem(title = stringResource(Res.string.created_at), value = m.createdAt.formatDateTime())
                    PListItem(title = stringResource(Res.string.updated_at), value = m.updatedAt.formatDateTime())
                    ImageMetaRows(path = m.path, loadMeta = { imagesVM.source.media.imageInfo(it) }, formatTexts = { mt -> formatImageMetaTexts(mt) })
                }
            }
            item { BottomSpace() }
        }
    }

    if (showQrScanResult) {
        QrScanResultBottomSheet(
            qrScanResult,
            title = stringResource(Res.string.scan_result),
            copyLabel = stringResource(Res.string.scan_result),
        ) { showQrScanResult = false }
    }
}
