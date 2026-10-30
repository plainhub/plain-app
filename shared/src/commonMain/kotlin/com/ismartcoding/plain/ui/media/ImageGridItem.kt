package com.ismartcoding.plain.ui.media

import androidx.compose.runtime.getValue

import com.ismartcoding.plain.models.Image as MediaImage
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
import com.ismartcoding.plain.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.media.getMediaItemUriString
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.media.viewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.media.viewer.previewer.TransformImageViewWithUri
import com.ismartcoding.plain.ui.media.viewer.previewer.rememberTransformItemState
import com.ismartcoding.plain.ui.casting.CastViewModel
import com.ismartcoding.plain.ui.images.ImagesViewModel
import com.ismartcoding.plain.ui.media.MediaPreviewData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageGridItem(
    scope: CoroutineScope,
    modifier: Modifier = Modifier,
    imagesVM: ImagesViewModel,
    castVM: CastViewModel,
    m: MediaImage,
    showSize: Boolean,
    previewerState: MediaPreviewerState,
    dragSelectState: DragSelectState,
    widthPx: Int,
) {
    val isSelected by remember { derivedStateOf { dragSelectState.isSelected(m.id) } }
    val inSelectionMode = dragSelectState.selectMode
    val selected = isSelected || imagesVM.selectedItem.value?.id == m.id
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
                        scope.launch {
                            withIO { MediaPreviewData.setDataAsync(itemState, imagesVM.itemsFlow.value, m, imagesVM.source) }
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
                    imagesVM.selectedItem.value = m
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
            uri = getMediaItemUriString(imagesVM.dataType, m.id, m.path),
            path = m.path,
            fileName = m.path.getFilenameFromPath(),
            key = m.id,
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
                SizeLabel(text = m.size.formatBytes())
            }
        }
    }
}
