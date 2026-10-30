package com.ismartcoding.plain.ui.media.viewer

import com.ismartcoding.plain.features.files.extension

import com.ismartcoding.plain.models.File as FileEntry

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.ismartcoding.plain.models.File
import com.ismartcoding.plain.ui.resources.image as ui_drawable_image

import com.ismartcoding.plain.models.Image as MediaImage
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.features.chat.models.MessageFile
import com.ismartcoding.plain.models.Image
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.image
import com.ismartcoding.plain.i18n.more_info
import com.ismartcoding.plain.i18n.rotate
import com.ismartcoding.plain.i18n.save
import com.ismartcoding.plain.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.platform.media.canSavePreviewMedia
import com.ismartcoding.plain.platform.media.savePreviewMedia
import com.ismartcoding.plain.ui.components.ControlChipIconButton
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.components.SaveToSheet
import com.ismartcoding.plain.ui.media.viewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.casting.CastViewModel
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.ellipsis as ui_drawable_ellipsis
import com.ismartcoding.plain.ui.resources.rotate_cw_square as ui_drawable_rotate_cw_square
import com.ismartcoding.plain.ui.resources.save as ui_drawable_save
import kotlinx.coroutines.launch
import com.ismartcoding.plain.ui.state.launchSafe
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
            ControlChipIconButton(icon = UiRes.drawable.ui_drawable_rotate_cw_square, contentDescription = stringResource(Res.string.rotate)) {
                scope.launch {
                    state.viewerContainerState?.viewerState?.let { viewer ->
                        viewer.rotation.animateTo(viewer.rotation.value + 90f)
                    }
                }
            }
            if (canSavePreviewMedia(m.source) && m.data !is MediaImage && m.data !is FileEntry) {
                HorizontalSpace(dp = 20.dp)
                ControlChipIconButton(icon = UiRes.drawable.ui_drawable_save, contentDescription = stringResource(Res.string.save)) {
                    showSaveSheet = true
                }
            }
            HorizontalSpace(dp = 20.dp)
            ControlChipIconButton(icon = UiRes.drawable.ui_drawable_ellipsis, contentDescription = stringResource(Res.string.more_info)) {
                state.showMediaInfo = true
            }
        }
    }

    if (showSaveSheet) {
        SaveToSheet(
            source = m.source,
            title = (m.data as? MessageFile)?.fileName?.takeIf { it.isNotEmpty() }
                ?: m.path.getFilenameFromPath().ifEmpty { stringResource(Res.string.image) },
            onDismiss = { showSaveSheet = false },
            onDownloads = {
                showSaveSheet = false
                scope.launchSafe { savePreviewMedia(m, null) }
            },
            onDirectory = { dir ->
                showSaveSheet = false
                scope.launchSafe { savePreviewMedia(m, dir) }
            },
        )
    }
}
