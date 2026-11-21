package com.ismartcoding.plain.ui.files.components

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.models.File
import com.ismartcoding.plain.ui.media.viewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.media.viewer.previewer.rememberTransformItemState

@Composable
internal fun FileTrashItem(
    file: File,
    isSelected: Boolean,
    isSelectMode: Boolean,
    previewerState: MediaPreviewerState,
    onToggle: () -> Unit,
) {
    FileListRow(
        file = file, itemState = rememberTransformItemState(), previewerState = previewerState,
        isSelected = isSelected, isSelectMode = isSelectMode,
        onClick = onToggle, onLongClick = onToggle,
    )
}
