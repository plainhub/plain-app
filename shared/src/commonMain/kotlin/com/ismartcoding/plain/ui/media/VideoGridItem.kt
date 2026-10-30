package com.ismartcoding.plain.ui.media

import androidx.compose.runtime.getValue

import com.ismartcoding.plain.models.Video as MediaVideo
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.lib.extensions.formatBytes
import com.ismartcoding.plain.lib.extensions.formatDurationMs
import com.ismartcoding.plain.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.lib.coMain
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.features.files.FileSortBy
import com.ismartcoding.plain.platform.media.getMediaItemUriString
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.media.viewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.media.viewer.previewer.TransformImageViewWithUri
import com.ismartcoding.plain.ui.media.viewer.previewer.rememberTransformItemState
import com.ismartcoding.plain.ui.casting.CastViewModel
import com.ismartcoding.plain.ui.media.MediaPreviewData
import com.ismartcoding.plain.ui.videos.VideosViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoGridItem(
    modifier: Modifier = Modifier,
    videosVM: VideosViewModel,
    castVM: CastViewModel,
    m: MediaVideo,
    showSize: Boolean,
    previewerState: MediaPreviewerState,
    dragSelectState: DragSelectState,
    widthPx: Int,
    sort: FileSortBy
) {
    val isSelected by remember { derivedStateOf { dragSelectState.isSelected(m.id) } }
    val inSelectionMode = dragSelectState.selectMode
    val selected = isSelected || videosVM.selectedItem.value?.id == m.id
    val itemState = rememberTransformItemState()
    Box(
        modifier = modifier
            .combinedClickable(
                onClick = {
                    if (castVM.castMode.value) {
                        castVM.cast(m.path)
                    } else if (inSelectionMode) {
                        dragSelectState.addSelected(m.id)
                    } else {
                        coMain {
                            withIO { MediaPreviewData.setDataAsync(itemState, videosVM.itemsFlow.value, m, videosVM.source) }
                            previewerState.openTransform(
                                index = MediaPreviewData.items.indexOfFirst { it.id == m.id },
                                itemState = itemState,
                            )
                        }
                    }
                },
                onLongClick = {
                    if (inSelectionMode) {
                        return@combinedClickable
                    }
                    videosVM.selectedItem.value = m
                },
            )
            .then(
                if (!inSelectionMode) {
                    Modifier
                } else {
                    Modifier.toggleable(
                        value = selected,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onValueChange = { toggled ->
                            if (toggled) {
                                dragSelectState.addSelected(m.id)
                            } else {
                                dragSelectState.removeSelected(m.id)
                            }
                        }
                    )
                },
            ),
    ) {
        TransformImageViewWithUri(
            modifier = Modifier.size(with(LocalDensity.current) { widthPx.toDp() }),
            path = m.path,
            fileName = m.path.getFilenameFromPath(),
            key = m.id,
            uri = getMediaItemUriString(videosVM.dataType, m.id, m.path),
            itemState = itemState,
            previewerState = previewerState,
            widthPx = widthPx
        )

        if (selected) {
            SelectedOverlay()
        } else if (castVM.castMode.value) {
            CastModeOverlay()
        }

        if (inSelectionMode) {
            Box(modifier = Modifier.align(Alignment.TopStart).padding(2.dp)) {
                SelectionCheckbox(selected = selected, onClick = { dragSelectState.select(m.id) })
            }
        }
        if (showSize) {
            Box(modifier = Modifier.align(Alignment.BottomEnd)) {
                SizeLabel(
                    text = if (setOf(FileSortBy.SIZE_ASC, FileSortBy.SIZE_DESC).contains(sort))
                        m.size.formatBytes() else m.durationMs.formatDurationMs()
                )
            }
        }
    }
}
