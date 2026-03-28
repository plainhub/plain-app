package com.ismartcoding.plain.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ismartcoding.lib.extensions.formatBytes
import com.ismartcoding.lib.extensions.getFilenameFromPath
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.data.DImage
import com.ismartcoding.plain.features.media.ImageMediaStoreHelper
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.TransformImageView
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.rememberTransformItemState
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.ImagesViewModel
import com.ismartcoding.plain.ui.models.MediaPreviewData

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageGridItem(
    modifier: Modifier = Modifier,
    imagesVM: ImagesViewModel,
    castVM: CastViewModel,
    m: DImage,
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
                        coMain {
                            withIO { MediaPreviewData.setDataAsync(itemState, imagesVM.itemsFlow.value, m) }
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
        TransformImageView(
            modifier = Modifier.fillMaxSize().align(Alignment.Center).aspectRatio(1f),
            uri = ImageMediaStoreHelper.getItemUri(m.id),
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
            Box(modifier = Modifier.align(Alignment.TopStart)) {
                SelectionCheckbox(selected = selected, id = m.id, dragSelectState = dragSelectState)
            }
        }
        if (showSize) {
            Box(modifier = Modifier.align(Alignment.BottomEnd)) {
                SizeLabel(text = m.size.formatBytes())
            }
        }
    }
}