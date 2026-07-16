package com.ismartcoding.plain.ui.page.images

import com.ismartcoding.plain.i18n.*

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
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.lib.extensions.formatBytes
import com.ismartcoding.plain.lib.extensions.getMimeType
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.db.DTagRelation
import com.ismartcoding.plain.platform.formatDateTime
import com.ismartcoding.plain.platform.getImageMeta
import com.ismartcoding.plain.platform.getSvgSize
import com.ismartcoding.plain.platform.tryDecodeQrCode
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.components.FileRenameDialog
import com.ismartcoding.plain.platform.renameAndScanFile
import com.ismartcoding.plain.platform.formatImageMetaTexts
import com.ismartcoding.plain.platform.ImageMetaRows
import com.ismartcoding.plain.ui.components.QrScanResultBottomSheet
import com.ismartcoding.plain.ui.components.TagSelector
import com.ismartcoding.plain.ui.models.ImagesViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ViewImageBottomSheet(
    imagesVM: ImagesViewModel,
    tagsVM: TagsViewModel,
    tagsMap: Map<String, List<DTagRelation>>,
    tagsState: List<DTag>,
    dragSelectState: DragSelectState,
) {
    val m = imagesVM.selectedItem.value ?: return
    val onDismiss = { imagesVM.selectedItem.value = null }
    var viewSize by remember { mutableStateOf(m.getRotatedSize()) }
    var showQrScanResult by remember { mutableStateOf(false) }
    var qrScanResult by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.Default) {
            if (m.path.endsWith(".svg", true)) {
                viewSize = getSvgSize(m.path)
            }
            try {
                val result = tryDecodeQrCode(m.path)
                if (result != null) {
                    qrScanResult = result
                }
            } catch (e: Exception) {
            }
        }
    }

    if (imagesVM.showRenameDialog.value) {
        FileRenameDialog(path = m.path, onDismiss = {
            imagesVM.showRenameDialog.value = false
        }, onRename = { p, name -> renameAndScanFile(p, name) }, onRenamed = {
            imagesVM.loadAsync(tagsVM)
            onDismiss()
        })
    }

    PModalBottomSheet(onDismissRequest = { onDismiss() }) {
        LazyColumn {
            item { VerticalSpace(32.dp) }
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
                    Subtitle(text = stringResource(Res.string.tags))
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
                PCard {
                    PListItem(title = stringResource(Res.string.file_size), value = m.size.formatBytes())
                    PListItem(title = stringResource(Res.string.type), value = m.path.getMimeType())
                    PListItem(title = stringResource(Res.string.dimensions), value = "${viewSize.width}\u00d7${viewSize.height}")
                    PListItem(title = stringResource(Res.string.created_at), value = m.createdAt.formatDateTime())
                    PListItem(title = stringResource(Res.string.updated_at), value = m.updatedAt.formatDateTime())
                    ImageMetaRows(path = m.path, loadMeta = { getImageMeta(it) }, formatTexts = { mt -> formatImageMetaTexts(mt) })
                }
            }
            item { BottomSpace() }
        }
    }

    if (showQrScanResult) {
        QrScanResultBottomSheet(qrScanResult) { showQrScanResult = false }
    }
}
