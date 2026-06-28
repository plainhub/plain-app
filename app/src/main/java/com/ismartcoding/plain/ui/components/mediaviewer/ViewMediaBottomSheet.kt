package com.ismartcoding.plain.ui.components.mediaviewer

import com.ismartcoding.plain.i18n.*

import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.lib.extensions.formatBytes
import com.ismartcoding.plain.lib.extensions.isUrl
import java.io.File
import com.ismartcoding.plain.data.DImage
import com.ismartcoding.plain.data.DVideo
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.db.DTagRelation
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.helpers.QrCodeScanHelper
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.FileRenameDialog
import com.ismartcoding.plain.ui.components.ImageMetaRows
import com.ismartcoding.plain.ui.components.QrScanResultBottomSheet
import com.ismartcoding.plain.ui.components.TagSelector
import com.ismartcoding.plain.ui.components.VideoMetaRows
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
    var showRenameDialog by remember { mutableStateOf(false) }
    var showQrScanResult by remember { mutableStateOf(false) }
    var qrScanResult by remember { mutableStateOf("") }
    // Local copy of `m.size` so the bottom sheet recomposes when the lazy
    // stat below fills in the missing byte count for `app://` images that
    // arrived via the markdown preview path. `PreviewItem.size` is a plain
    // `var`, so writing back to it from a coroutine wouldn't trigger a
    // recomposition — this local `MutableLongState` does.
    var displaySize by remember(m) { mutableLongStateOf(m.size) }

    if (showRenameDialog) {
        FileRenameDialog(path = m.path, onDismiss = { showRenameDialog = false }, onDoneAsync = {
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
                        if (result != null) { qrScanResult = result.text }
                    }
                } catch (e: Exception) { }
            }
        }
        // `PreviewItem` rows built from raw markdown (`<img src="app://…">` or
        // `![alt](app://…)`) have no associated `DMessageFile` / `DImage`, so the
        // caller never gets a chance to set `size` at construction time. Stat
        // the file lazily here so we don't pay the cost for every image in the
        // document on every recomposition — only the one the user is currently
        // inspecting. Remote URLs are skipped; their "size" would just be the
        // byte count of an HTTP download, which the previewer doesn't surface.
        if (displaySize <= 0L && !m.path.isUrl()) {
            scope.launch(Dispatchers.IO) {
                val stat = runCatching { File(m.path).takeIf { it.exists() }?.length() ?: 0L }
                    .getOrDefault(0L)
                if (stat > 0L) displaySize = stat
            }
        }
    }

    PModalBottomSheet(onDismissRequest = { onDismiss() }) {
        LazyColumn {
            item { VerticalSpace(32.dp) }
            if (m.data is DImage || m.data is DVideo) {
                item {
                    ViewMediaActionButtons(m = m, qrScanResult = qrScanResult,
                        onShowQrScanResult = { showQrScanResult = true },
                        onShowRenameDialog = { showRenameDialog = true },
                        deleteAction = deleteAction, onDismiss = onDismiss)
                }
                item {
                    VerticalSpace(dp = 16.dp)
                    Subtitle(text = stringResource(Res.string.tags))
                    TagSelector(data = m.data, tagsVM = tagsVM!!, tagsMap = tagsMap!!,
                        tagsState = tagsState, onChangedAsync = { onTagsChangedAsync() })
                    VerticalSpace(dp = 16.dp)
                }
            }
            item { ViewMediaPathCard(m = m) }
            item {
                VerticalSpace(dp = 16.dp)
                PCard {
                    PListItem(title = stringResource(Res.string.file_size), value = displaySize.formatBytes())
                    val mimeType = m.getMimeType()
                    if (mimeType.isNotEmpty()) {
                        PListItem(title = stringResource(Res.string.type), value = mimeType)
                    }
                    val intrinsicSize = m.intrinsicSize
                    if (intrinsicSize.width > 0 && intrinsicSize.height > 0) {
                        PListItem(title = stringResource(Res.string.dimensions), value = "${intrinsicSize.width}\u00d7${intrinsicSize.height}")
                    }
                    if (m.data is DImage) {
                        PListItem(title = stringResource(Res.string.created_at), value = m.data.createdAt.formatDateTime())
                        PListItem(title = stringResource(Res.string.updated_at), value = m.data.updatedAt.formatDateTime())
                        ImageMetaRows(path = m.path)
                    } else if (m.data is DVideo) {
                        PListItem(title = stringResource(Res.string.created_at), value = m.data.createdAt.formatDateTime())
                        PListItem(title = stringResource(Res.string.updated_at), value = m.data.updatedAt.formatDateTime())
                        VideoMetaRows(path = m.path)
                    } else if (m.path.isUrl()) {
                    } else if (mimeType.startsWith("image/")) {
                        ImageMetaRows(path = m.path)
                    } else if (mimeType.startsWith("video/")) {
                        VideoMetaRows(path = m.path)
                    }
                }
            }
            item { BottomSpace() }
        }
    }

    if (showQrScanResult) {
        QrScanResultBottomSheet(qrScanResult) { showQrScanResult = false }
    }
}
