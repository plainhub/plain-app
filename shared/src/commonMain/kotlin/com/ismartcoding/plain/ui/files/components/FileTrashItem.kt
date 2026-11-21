package com.ismartcoding.plain.ui.files.components

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.features.files.TrashedFile
import com.ismartcoding.plain.features.files.TrashedFileType
import com.ismartcoding.plain.ui.media.viewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.media.viewer.previewer.rememberTransformItemState

@Composable
internal fun FileTrashItem(
    item: TrashedFile,
    isSelected: Boolean,
    isSelectMode: Boolean,
    previewerState: MediaPreviewerState,
    onToggle: () -> Unit,
) {
    FileListRow(
        title = item.displayName, subtitle = item.originalPath, subtitleMaxLines = 2,
        isSelected = isSelected, isSelectMode = isSelectMode,
        onClick = onToggle, onLongClick = onToggle,
    ) {
        FileListItemThumbnail(
            item.displayName, item.trashedPath, item.type == TrashedFileType.DIR,
            rememberTransformItemState(), previewerState,
        )
    }
}
