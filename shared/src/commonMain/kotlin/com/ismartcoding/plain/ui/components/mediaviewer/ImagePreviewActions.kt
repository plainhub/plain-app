package com.ismartcoding.plain.ui.components.mediaviewer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.data.DImage
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.ellipsis
import com.ismartcoding.plain.i18n.more_info
import com.ismartcoding.plain.i18n.rotate
import com.ismartcoding.plain.i18n.rotate_cw_square
import com.ismartcoding.plain.i18n.image
import com.ismartcoding.plain.i18n.save
import com.ismartcoding.plain.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.platform.canSavePreviewMedia
import com.ismartcoding.plain.platform.savePreviewMedia
import com.ismartcoding.plain.ui.base.ControlChipIconButton
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.components.SaveToSheet
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.models.CastViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun ImagePreviewActions(
    castViewModel: CastViewModel,
    m: PreviewItem,
    state: MediaPreviewerState,
) {
    val scope = rememberCoroutineScope()
    var showSaveSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 32.dp)
            .alpha(state.uiAlpha.value),
    ) {
        if (!state.showActions) return
        Row(
            modifier = Modifier.align(Alignment.BottomEnd)
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            ControlChipIconButton(icon = Res.drawable.rotate_cw_square, contentDescription = stringResource(Res.string.rotate)) {
                scope.launch {
                    state.viewerContainerState?.viewerState?.let { viewer ->
                        viewer.rotation.animateTo(viewer.rotation.value + 90f)
                    }
                }
            }
            if (canSavePreviewMedia && m.data !is DImage && m.data !is DFile) {
                HorizontalSpace(dp = 20.dp)
                ControlChipIconButton(icon = Res.drawable.save, contentDescription = stringResource(Res.string.save)) {
                    showSaveSheet = true
                }
            }
            HorizontalSpace(dp = 20.dp)
            ControlChipIconButton(icon = Res.drawable.ellipsis, contentDescription = stringResource(Res.string.more_info)) {
                state.showMediaInfo = true
            }
        }
    }

    if (showSaveSheet) {
        SaveToSheet(
            title = (m.data as? DMessageFile)?.fileName?.takeIf { it.isNotEmpty() }
                ?: m.path.getFilenameFromPath().ifEmpty { stringResource(Res.string.image) },
            onDismiss = { showSaveSheet = false },
            onDownloads = {
                showSaveSheet = false
                scope.launch { savePreviewMedia(m, null) }
            },
            onDirectory = { dir ->
                showSaveSheet = false
                scope.launch { savePreviewMedia(m, dir) }
            },
        )
    }
}
