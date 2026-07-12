package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.db.DTagRelation
import com.ismartcoding.plain.ui.components.mediaviewer.PreviewItem
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.models.TagsViewModel

/**
 * iOS stub. The full-screen media previewer relies on Android-specific video
 * decoding and ContentResolver-backed media; on iOS it is a no-op.
 */
@Composable
actual fun MediaPreviewer(
    state: MediaPreviewerState,
    castVM: CastViewModel,
    tagsVM: TagsViewModel?,
    tagsMap: Map<String, List<DTagRelation>>?,
    tagsState: List<DTag>,
    onRenamed: () -> Unit,
    deleteAction: (PreviewItem) -> Unit,
    onTagsChanged: () -> Unit,
) {
    // No-op on iOS
}
